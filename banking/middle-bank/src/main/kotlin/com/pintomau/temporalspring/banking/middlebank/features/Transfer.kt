package com.pintomau.temporalspring.banking.middlebank.features

import com.pintomau.temporalspring.banking.middlebank.clients.*
import com.pintomau.temporalspring.banking.middlebank.infra.messaging.objectMessageBuilder
import io.github.oshai.kotlinlogging.KotlinLogging
import io.temporal.activity.ActivityInterface
import io.temporal.activity.ActivityOptions
import io.temporal.activity.setRetryOptions
import io.temporal.api.enums.v1.WorkflowIdReusePolicy
import io.temporal.client.WorkflowClient
import io.temporal.client.WorkflowExecutionAlreadyStarted
import io.temporal.client.newWorkflowStub
import io.temporal.common.RetryOptions
import io.temporal.spring.boot.ActivityImpl
import io.temporal.spring.boot.WorkflowImpl
import io.temporal.workflow.*
import java.math.BigDecimal
import java.time.Duration
import java.util.*
import org.springframework.amqp.core.FanoutExchange
import org.springframework.amqp.core.MessagePropertiesBuilder
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

private val logger = KotlinLogging.logger {}
private const val TRANSFER_WORKFLOW_QUEUE = "transfer"

@RestController
class TransferController(
  private val initiateTransferHandler: InitiateTransferHandler,
) {

  @PostMapping("/transfer")
  fun transfer(@Validated @RequestBody transferAction: TransferAction): WorkflowAndRun {
    return initiateTransferHandler.handle(transferAction)
  }
}

data class TransferAction(
  val fromBank: BankType,
  val fromAccount: UUID,
  val toBank: BankType,
  val toAccount: UUID,
  val amount: BigDecimal,
  val requestId: String,
)

data class WorkflowAndRun(
  val workflowId: String,
  val workflowRun: String,
)

@Service
class InitiateTransferHandler(
  private val workflowClient: WorkflowClient,
) {

  fun handle(transferAction: TransferAction): WorkflowAndRun {
    val workflowStub =
      workflowClient.newWorkflowStub<TransferWorkflow> {
        setTaskQueue(TRANSFER_WORKFLOW_QUEUE)
        setWorkflowId(transferAction.requestId)
        setWorkflowIdReusePolicy(
          WorkflowIdReusePolicy.WORKFLOW_ID_REUSE_POLICY_ALLOW_DUPLICATE_FAILED_ONLY,
        )
      }

    try {
      val execution = WorkflowClient.start(workflowStub::transfer, transferAction)
      return WorkflowAndRun(execution.workflowId, execution.runId)
    } catch (e: WorkflowExecutionAlreadyStarted) {
      return WorkflowAndRun(e.execution.workflowId, e.execution.runId)
    }
  }
}

@WorkflowInterface
fun interface TransferWorkflow {

  @WorkflowMethod fun transfer(transferWorkflowCommand: TransferAction): WorkflowResult?
}

@ActivityInterface
interface TransferActivities {
  fun withdraw(transferWorkflowCommand: TransferAction): BankClientAccount

  fun undoWithdraw(transferWorkflowCommand: TransferAction): BankClientAccount

  fun deposit(transferWorkflowCommand: TransferAction): BankClientAccount

  fun undoDeposit(transferWorkflowCommand: TransferAction): BankClientAccount

  fun transferFinished(transferWorkflowCommand: TransferAction)
}

/** These options apply generally to all activities */
private val defaultActivityOptions = ActivityOptions {
  setRetryOptions(
    RetryOptions {
      setInitialInterval(Duration.ofSeconds(1))
      setMaximumInterval(Duration.ofSeconds(100))
      setBackoffCoefficient(2.0)
      setMaximumAttempts(3)
    },
  )
  // Maximum time of a single Activity attempt.
  setStartToCloseTimeout(Duration.ofSeconds(2))
}

/** These options apply to the named activities */
private val activityMethodOptions =
  mapOf(
    "Withdraw" to ActivityOptions { setRetryOptions { setMaximumAttempts(10) } },
    "Deposit" to ActivityOptions { setRetryOptions { setMaximumAttempts(1) } },
  )

@WorkflowImpl(taskQueues = [TRANSFER_WORKFLOW_QUEUE])
class TransferWorkflowImpl(
  private val transferActivities: TransferActivities =
    Workflow.newActivityStub(
      TransferActivities::class.java,
      defaultActivityOptions,
      activityMethodOptions,
    ),
) : TransferWorkflow {

  override fun transfer(transferWorkflowCommand: TransferAction): WorkflowResult? {
    val saga =
      Saga(Saga.Options.Builder().setParallelCompensation(false).setContinueWithError(true).build())

    try {
      val withdrawnAccount = transferActivities.withdraw(transferWorkflowCommand)
      saga.addCompensation(transferActivities::undoWithdraw, transferWorkflowCommand)

      val depositedAccount = transferActivities.deposit(transferWorkflowCommand)
      saga.addCompensation(transferActivities::undoDeposit, transferWorkflowCommand)

      // dumb example of timers
      // there's also Workflow.await, signals, etc...
      val promiseToMessage =
        Workflow.newTimer(Duration.ofSeconds(1)).thenApply {
          // Notice: no need for outbox pattern
          transferActivities.transferFinished(transferWorkflowCommand)
        }

      val promiseToSucceed =
        Workflow.newTimer(Duration.ofSeconds(2)).thenApply {
          logger.info {
            "Successfully transferred transfer workflow ${transferWorkflowCommand.requestId}"
          }
        }

      Promise.allOf(promiseToMessage, promiseToSucceed).get()

      logger.info { "Returning from ${transferWorkflowCommand.requestId}" }

      return WorkflowResult(withdrawnAccount, depositedAccount)
    } catch (e: Exception) {
      saga.compensate()
    }

    return null
  }
}

data class WorkflowResult(
  val withdrawnAccount: BankClientAccount,
  val depositedAccount: BankClientAccount,
)

@ActivityImpl(taskQueues = [TRANSFER_WORKFLOW_QUEUE])
@Component
class TransferActivitiesImpl(
  private val bankClientResolver: BankClientResolver,
  private val rabbitTemplate: RabbitTemplate,
  private val transferFanout: FanoutExchange,
) : TransferActivities {

  override fun withdraw(transferWorkflowCommand: TransferAction): BankClientAccount {
    logger.info { "Withdrawing $transferWorkflowCommand" }
    return bankClientResolver(transferWorkflowCommand.fromBank)
      .withdraw(
        transferWorkflowCommand.fromAccount,
        WithdrawRequest(
          transferWorkflowCommand.amount,
          "withdraw:${transferWorkflowCommand.requestId}",
        ),
      )
      .also { logger.info { "Finished withdrawing $transferWorkflowCommand" } }
  }

  override fun undoWithdraw(transferWorkflowCommand: TransferAction): BankClientAccount {
    logger.info { "Undoing withdrawing $transferWorkflowCommand" }
    return bankClientResolver(transferWorkflowCommand.fromBank)
      .deposit(
        transferWorkflowCommand.fromAccount,
        DepositRequest(
          transferWorkflowCommand.amount,
          "deposit:${transferWorkflowCommand.requestId}",
        ),
      )
      .also { logger.info { "Finished undoing withdrawing $transferWorkflowCommand" } }
  }

  override fun deposit(transferWorkflowCommand: TransferAction): BankClientAccount {
    logger.info { "Depositing $transferWorkflowCommand" }
    return bankClientResolver(transferWorkflowCommand.toBank)
      .deposit(
        transferWorkflowCommand.toAccount,
        DepositRequest(
          transferWorkflowCommand.amount,
          "deposit:${transferWorkflowCommand.requestId}",
        ),
      )
      .also { logger.info { "Finished depositing $transferWorkflowCommand" } }
  }

  override fun undoDeposit(transferWorkflowCommand: TransferAction): BankClientAccount {
    logger.info { "Undoing depositing $transferWorkflowCommand" }
    return bankClientResolver(transferWorkflowCommand.toBank)
      .withdraw(
        transferWorkflowCommand.toAccount,
        WithdrawRequest(
          transferWorkflowCommand.amount,
          "withdraw:${transferWorkflowCommand.requestId}",
        ),
      )
      .also { logger.info { "Finished undoing depositing $transferWorkflowCommand" } }
  }

  override fun transferFinished(transferWorkflowCommand: TransferAction) {
    logger.info { "Sending TransferFinished event to TransferFanout $transferWorkflowCommand" }
    val transferFinished =
      TransferFinished(
        transferWorkflowCommand.fromBank,
        transferWorkflowCommand.fromAccount,
        transferWorkflowCommand.toBank,
        transferWorkflowCommand.toAccount,
        transferWorkflowCommand.amount,
        transferWorkflowCommand.requestId,
      )
    val message =
      objectMessageBuilder(transferFinished)
        .andProperties(
          MessagePropertiesBuilder.newInstance()
            .setMessageId(transferFinished.requestId)
            .setCorrelationId(transferFinished.requestId)
            .build(),
        )
        .build()

    rabbitTemplate.send(transferFanout.name, "", message)
    logger.info {
      "Finished sending TransferFinished event to TransferFanout $transferWorkflowCommand"
    }
  }
}

data class TransferFinished(
  val fromBank: BankType,
  val fromAccount: UUID,
  val toBank: BankType,
  val toAccount: UUID,
  val amount: BigDecimal,
  // should be required. nullable for demo purposes
  val requestId: String? = null,
)

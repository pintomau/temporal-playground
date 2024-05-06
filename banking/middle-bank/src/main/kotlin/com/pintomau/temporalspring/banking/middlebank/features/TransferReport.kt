package com.pintomau.temporalspring.banking.middlebank.features

import io.github.oshai.kotlinlogging.KotlinLogging
import io.temporal.client.WorkflowClient
import io.temporal.client.WorkflowExecutionAlreadyStarted
import io.temporal.client.newWorkflowStub
import io.temporal.spring.boot.WorkflowImpl
import io.temporal.workflow.SignalMethod
import io.temporal.workflow.Workflow
import io.temporal.workflow.WorkflowInterface
import io.temporal.workflow.WorkflowMethod
import java.math.BigDecimal
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.EventListener
import org.springframework.messaging.Message
import org.springframework.stereotype.Component

private const val TRANSFER_REPORT_QUEUE = "transferReportQueue"
private const val WORKFLOW_ID = "transfer-report"

private val logger = KotlinLogging.logger {}

@Component
class TransferReportTransferFinishedListener(
  private val workflowClient: WorkflowClient,
) {

  @RabbitListener(queues = ["receiver.transfer_report_queue"])
  fun listen(transferFinished: Message<TransferFinished>) {
    logger.info { "appending report for: $transferFinished" }

    val newWorkflowStub = workflowClient.newWorkflowStub<TransferReportWorkflow>(WORKFLOW_ID)
    newWorkflowStub.addTransferSignal(transferFinished.payload)

    logger.info { "finished appending report for: $transferFinished" }
  }
}

@WorkflowInterface
interface TransferReportWorkflow {
  @WorkflowMethod fun start()

  @SignalMethod fun addTransferSignal(transferFinished: TransferFinished)
}

private const val ITEMS_TO_SUMMARIZE = 3

@WorkflowImpl(taskQueues = [TRANSFER_REPORT_QUEUE])
class TransferReportImpl : TransferReportWorkflow {

  private val logger = Workflow.getLogger(TransferReportImpl::class.java.getName())

  private var total = BigDecimal.ZERO
  private val transfers = mutableListOf<TransferFinished>()

  override fun start() {
    logger.info("adding initiating transfer report")
    while (true) {
      Workflow.await { transfers.size >= ITEMS_TO_SUMMARIZE }

      logger.info("Summarizing transfers: total: $total; transfers: $transfers")
      total = BigDecimal.ZERO
      transfers.clear()
    }
  }

  override fun addTransferSignal(transferFinished: TransferFinished) {
    total += transferFinished.amount
    transfers += transferFinished
  }
}

@Configuration
class TransferBootUp(
  private val workflowClient: WorkflowClient,
) {

  @EventListener
  fun onApplicationEvent(event: ApplicationReadyEvent) {
    val newWorkflowStub =
      workflowClient.newWorkflowStub<TransferReportWorkflow> {
        setTaskQueue(TRANSFER_REPORT_QUEUE)
        setWorkflowId(WORKFLOW_ID)
      }

    try {
      WorkflowClient.start(newWorkflowStub::start)
    } catch (e: WorkflowExecutionAlreadyStarted) {
      logger.info { "workflow 'transfer-report' had already started" }
    }

    //    val factory = WorkerFactory.newInstance(workflowClient)
    //    val worker = factory.newWorker(TRANSFER_REPORT_QUEUE)
    //    worker.registerWorkflowImplementationTypes(TransferReportImpl::class.java)
    //    factory.start()
  }
}

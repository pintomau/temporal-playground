@file:Suppress("DuplicatedCode")

package com.pintomau.temporalspring.banking.middlebank.features

import com.pintomau.temporalspring.banking.middlebank.clients.BankClientResolver
import com.pintomau.temporalspring.banking.middlebank.clients.BankType
import com.pintomau.temporalspring.banking.middlebank.clients.DepositRequest
import com.pintomau.temporalspring.banking.middlebank.clients.WithdrawRequest
import com.pintomau.temporalspring.banking.middlebank.infra.messaging.objectMessageBuilder
import java.math.BigDecimal
import java.util.*
import org.springframework.amqp.core.FanoutExchange
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Service
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class SimpleTransferController(
  private val transferHandler: TransferHandler,
) {

  @PostMapping("/simple-transfer")
  fun transfer(@Validated @RequestBody transferAction: SimpleTransferAction) {
    transferHandler.handle(transferAction)
  }
}

data class SimpleTransferAction(
  val fromBank: BankType,
  val fromAccount: UUID,
  val toBank: BankType,
  val toAccount: UUID,
  val amount: BigDecimal,
)

@Service
class TransferHandler(
  private val bankClientResolver: BankClientResolver,
  private val rabbitTemplate: RabbitTemplate,
  private val transferFanout: FanoutExchange,
) {

  fun handle(simpleTransferAction: SimpleTransferAction) {
    val bank1 = bankClientResolver(simpleTransferAction.fromBank)
    bank1.withdraw(simpleTransferAction.fromAccount, WithdrawRequest(simpleTransferAction.amount))

    val bank2 = bankClientResolver(simpleTransferAction.toBank)
    bank2.deposit(simpleTransferAction.toAccount, DepositRequest(simpleTransferAction.amount))

    val transferFinished =
      objectMessageBuilder(
          TransferFinished(
            simpleTransferAction.fromBank,
            simpleTransferAction.fromAccount,
            simpleTransferAction.toBank,
            simpleTransferAction.toAccount,
            simpleTransferAction.amount,
          ),
        )
        .build()
    rabbitTemplate.send(
      transferFanout.name,
      "",
      transferFinished,
    )
  }
}

@Service
class ResTransferHandler(
  private val bankClientResolver: BankClientResolver,
  private val rabbitTemplate: RabbitTemplate,
  private val transferFanout: FanoutExchange,
) {

  // retry on failure
  // retries should be idempotent/resume same workflow
  // @WorkflowMethod
  fun handle(simpleTransferAction: SimpleTransferAction) {
    // retry on failure
    // retries need to be idempotent
    val bank1 = bankClientResolver(simpleTransferAction.fromBank)
    // @ActivityMethod
    bank1.withdraw(simpleTransferAction.fromAccount, WithdrawRequest(simpleTransferAction.amount))
    // add compensation in case of forward errors

    // save state for observability, resuming

    // retry on failure
    // retries need to be idempotent
    val bank2 = bankClientResolver(simpleTransferAction.toBank)
    // @ActivityMethod
    bank2.deposit(simpleTransferAction.toAccount, DepositRequest(simpleTransferAction.amount))
    // add compensation in case of forward errors

    // save state for observability, resuming

    // outbox pattern for message sending?
    // message can/should also be idempotent
    val message =
      objectMessageBuilder(
          TransferFinished(
            simpleTransferAction.fromBank,
            simpleTransferAction.fromAccount,
            simpleTransferAction.toBank,
            simpleTransferAction.toAccount,
            simpleTransferAction.amount,
          ),
        )
        .build()

    // @ActivityMethod
    rabbitTemplate.send(
      transferFanout.name,
      "",
      message,
    )
  }
}

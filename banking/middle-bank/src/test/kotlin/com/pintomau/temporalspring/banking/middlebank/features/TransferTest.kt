package com.pintomau.temporalspring.banking.middlebank.features

import com.fasterxml.jackson.databind.ObjectMapper
import com.pintomau.temporalspring.banking.middlebank.clients.BankClientAccount
import com.pintomau.temporalspring.banking.middlebank.clients.BankType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.temporal.client.WorkflowClientOptions
import io.temporal.common.converter.DefaultDataConverter
import io.temporal.common.converter.JacksonJsonPayloadConverter
import io.temporal.testing.TestWorkflowEnvironment
import io.temporal.testing.TestWorkflowExtension
import io.temporal.worker.Worker
import java.math.BigDecimal
import java.util.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder

class TransferTest {

  @RegisterExtension
  private val testWorkflowExtension =
    TestWorkflowExtension.newBuilder()
      .registerWorkflowImplementationTypes(TransferWorkflowImpl::class.java)
      .setWorkflowClientOptions(
        WorkflowClientOptions {
          val mapper = Jackson2ObjectMapperBuilder.json().build<ObjectMapper>()
          val dataConverter =
            DefaultDataConverter.newDefaultInstance()
              .withPayloadConverterOverrides(
                JacksonJsonPayloadConverter(mapper),
              )

          setDataConverter(dataConverter)
        },
      )
      .setDoNotStart(true)
      .build()

  @Test
  fun `test Saga`(
    testEnv: TestWorkflowEnvironment,
    worker: Worker,
    transferWorkflow: TransferWorkflow,
  ) {
    // given
    val transferAction =
      TransferAction(
        BankType.A,
        UUID.randomUUID(),
        BankType.B,
        UUID.randomUUID(),
        BigDecimal.TEN,
        "request-id",
      )

    val transferActivities = mockk<TransferActivities>()
    every { transferActivities.withdraw(transferAction) } throws
      // test retry works
      RuntimeException("Something wrong with Withdraw 1") andThenThrows
      RuntimeException("Something wrong with Withdraw 2") andThenThrows
      RuntimeException("Something wrong with Withdraw 3") andThen
      BankClientAccount(
        transferAction.toAccount,
        BigDecimal.ZERO,
      )

    every { transferActivities.deposit(transferAction) } returns
      BankClientAccount(
        transferAction.fromAccount,
        BigDecimal.TEN,
      )

    every { transferActivities.transferFinished(transferAction) } returns Unit

    // when
    worker.registerActivitiesImplementations(transferActivities)
    testEnv.start()
    val (withdrawnAccount, depositedAccount) = transferWorkflow.transfer(transferAction)!!

    // then
    assertThat(withdrawnAccount)
      .isEqualTo(
        BankClientAccount(
          transferAction.toAccount,
          BigDecimal.ZERO,
        ),
      )
    assertThat(depositedAccount)
      .isEqualTo(
        BankClientAccount(
          transferAction.fromAccount,
          BigDecimal.TEN,
        ),
      )

    verify(inverse = true) { transferActivities.undoDeposit(any()) }
    verify(inverse = true) { transferActivities.undoWithdraw(any()) }
  }

  @Test
  fun `test compensations`(
    testEnv: TestWorkflowEnvironment,
    worker: Worker,
    transferWorkflow: TransferWorkflow,
  ) {
    // given
    val transferAction =
      TransferAction(
        BankType.A,
        UUID.randomUUID(),
        BankType.B,
        UUID.randomUUID(),
        BigDecimal.TEN,
        "request-id",
      )

    val transferActivities = mockk<TransferActivities>()
    every { transferActivities.withdraw(transferAction) } returns
      BankClientAccount(
        transferAction.toAccount,
        BigDecimal.ZERO,
      )
    every { transferActivities.undoWithdraw(transferAction) } returns
      BankClientAccount(
        transferAction.toAccount,
        BigDecimal.TEN,
      )

    every { transferActivities.deposit(transferAction) } returns
      BankClientAccount(
        transferAction.fromAccount,
        BigDecimal.TEN,
      )
    every { transferActivities.undoDeposit(transferAction) } returns
      BankClientAccount(
        transferAction.fromAccount,
        BigDecimal.ZERO,
      )

    every { transferActivities.transferFinished(transferAction) } throws
      RuntimeException("Something wrong with TransferFinished")

    // when
    worker.registerActivitiesImplementations(transferActivities)
    testEnv.start()
    val result = transferWorkflow.transfer(transferAction)

    // then
    assertThat(result).isNull()

    verify { transferActivities.undoWithdraw(transferAction) }
    verify { transferActivities.undoDeposit(transferAction) }
  }
}

package com.pintomau.temporalspring.banking.banka.features

import com.pintomau.temporalspring.banking.banka.core.Account
import com.pintomau.temporalspring.banking.banka.core.AccountRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import java.math.BigDecimal
import java.util.*
import org.springframework.stereotype.Service
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

private val logger = KotlinLogging.logger {}

@RestController
class WithdrawController(
  private val withdrawHandler: WithdrawHandler,
) {

  @PostMapping("{accountId}/withdraw")
  fun withdraw(
    @PathVariable("accountId") accountId: UUID,
    @Validated @RequestBody action: WithdrawAction,
  ): Account {
    logger.info { "Withdrawing $action for account $accountId" }
    return withdrawHandler.handle(accountId, action)
  }
}

data class WithdrawAction(
  val amount: BigDecimal,
  val requestId: String,
)

@Service
class WithdrawHandler(
  private val accountRepository: AccountRepository,
) {

  fun handle(accountId: UUID, action: WithdrawAction): Account {
    val account = accountRepository.getById(accountId)
    account.withdraw(action.amount)
    return accountRepository.replace(account, action.requestId)
  }
}

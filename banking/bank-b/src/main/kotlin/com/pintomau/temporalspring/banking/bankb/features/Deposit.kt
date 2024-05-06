package com.pintomau.temporalspring.banking.bankb.features

import com.pintomau.temporalspring.banking.bankb.core.Account
import com.pintomau.temporalspring.banking.bankb.core.AccountRepository
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
class DepositController(
  private val depositHandler: DepositHandler,
) {

  @PostMapping("{accountId}/deposit")
  fun deposit(
    @PathVariable("accountId") accountId: UUID,
    @Validated @RequestBody action: DepositAction,
  ): Account {
    logger.info { "Depositing $action for account $accountId" }
    return depositHandler.handle(accountId, action)
  }
}

data class DepositAction(
  val amount: BigDecimal,
  val requestId: String,
)

@Service
class DepositHandler(
  private val accountRepository: AccountRepository,
) {

  fun handle(accountId: UUID, depositAction: DepositAction): Account {
    val account = accountRepository.getById(accountId)
    account.deposit(depositAction.amount)
    return accountRepository.replace(account, depositAction.requestId)
  }
}

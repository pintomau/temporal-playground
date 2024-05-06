package com.pintomau.temporalspring.banking.bankb.features

import com.pintomau.temporalspring.banking.bankb.core.Account
import com.pintomau.temporalspring.banking.bankb.core.AccountRepository
import java.util.*
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class CreateAccountController(
  private val createAccountHandler: CreateAccountHandler,
) {

  @PostMapping fun createAccount() = createAccountHandler.handle()
}

@Service
class CreateAccountHandler(
  private val repository: AccountRepository,
) {
  fun handle() = repository.create(Account(UUID.randomUUID()))
}

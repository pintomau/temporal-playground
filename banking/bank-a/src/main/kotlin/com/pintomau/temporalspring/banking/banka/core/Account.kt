package com.pintomau.temporalspring.banking.banka.core

import com.fasterxml.jackson.annotation.JsonIgnore
import java.math.BigDecimal
import java.util.*
import org.springframework.data.annotation.Id

class Account(
  @Id val id: UUID,
  balance: BigDecimal = BigDecimal.ZERO,
  @get:JsonIgnore internal val requestIds: ArrayDeque<String> = ArrayDeque(REQUEST_IDS + 1),
) {

  var balance: BigDecimal = balance
    private set

  fun deposit(amount: BigDecimal): BigDecimal {
    balance += amount
    return balance
  }

  fun withdraw(amount: BigDecimal): BigDecimal {
    balance -= amount
    return balance
  }
}

package com.pintomau.temporalspring.banking.middlebank.clients

import com.fasterxml.jackson.databind.ObjectMapper
import feign.Feign
import feign.Headers
import feign.Param
import feign.RequestLine
import feign.jackson.JacksonDecoder
import feign.jackson.JacksonEncoder
import java.math.BigDecimal
import java.util.*
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

enum class BankType {
  A,
  B,
}

@Headers("Content-Type: application/json")
sealed interface BankClient {

  fun deposit(
    @Param("accountId") accountId: UUID,
    depositRequest: DepositRequest,
  ): BankClientAccount

  fun withdraw(
    @Param("accountId") accountId: UUID,
    withdrawRequest: WithdrawRequest,
  ): BankClientAccount
}

data class DepositRequest(
  val amount: BigDecimal,
  // remove nullable; just for example
  val requestId: String? = null,
)

data class WithdrawRequest(
  val amount: BigDecimal,
  // remove nullable; just for example
  val requestId: String? = null,
)

data class BankClientAccount(
  val id: UUID,
  val balance: BigDecimal,
)

interface BankAClient : BankClient {

  @RequestLine(
    "POST /bank-a/{accountId}/deposit",
  )
  override fun deposit(
    @Param("accountId") accountId: UUID,
    depositRequest: DepositRequest,
  ): BankClientAccount

  @RequestLine(
    "POST /bank-a/{accountId}/withdraw",
  )
  override fun withdraw(
    @Param("accountId") accountId: UUID,
    withdrawRequest: WithdrawRequest,
  ): BankClientAccount
}

interface BankBClient : BankClient {

  @RequestLine(
    "POST /bank-b/{accountId}/deposit",
  )
  override fun deposit(
    @Param("accountId") accountId: UUID,
    depositRequest: DepositRequest,
  ): BankClientAccount

  @RequestLine(
    "POST /bank-b/{accountId}/withdraw",
  )
  override fun withdraw(
    @Param("accountId") accountId: UUID,
    withdrawRequest: WithdrawRequest,
  ): BankClientAccount
}

typealias BankClientResolver = (BankType) -> BankClient

@Configuration
class BankClientsConfiguration {

  @Bean
  fun bankAClient(objectMapper: ObjectMapper): BankAClient {
    return Feign.builder()
      .encoder(JacksonEncoder(objectMapper))
      .decoder(JacksonDecoder(objectMapper))
      .target(BankAClient::class.java, "http://localhost:8081")
  }

  @Bean
  fun bankBClient(objectMapper: ObjectMapper): BankBClient {
    return Feign.builder()
      .encoder(JacksonEncoder(objectMapper))
      .decoder(JacksonDecoder(objectMapper))
      .target(BankBClient::class.java, "http://localhost:8082")
  }

  @Bean
  fun bankClientResolver(bankAClient: BankAClient, bankBClient: BankBClient): BankClientResolver = {
    when (it) {
      BankType.A -> bankAClient
      BankType.B -> bankBClient
    }
  }
}

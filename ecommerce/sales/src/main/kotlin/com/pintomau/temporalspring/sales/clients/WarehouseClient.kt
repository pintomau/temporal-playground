package com.pintomau.temporalspring.sales.clients

import com.fasterxml.jackson.databind.ObjectMapper
import com.pintomau.temporalspring.sales.features.AddLineItemWorkflowCommand
import feign.Feign
import feign.Headers
import feign.Param
import feign.RequestLine
import feign.jackson.JacksonDecoder
import feign.jackson.JacksonEncoder
import java.util.*
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Headers("Content-Type: application/json")
interface WarehouseClient {

  @RequestLine(
    "POST /warehouse/carts/{cartId}/add-line-item",
  )
  fun addLineItem(
    @Param("cartId") cartId: UUID,
    command: AddLineItemWorkflowCommand,
  ): Any

  @RequestLine(
    "POST /warehouse/carts/{cartId}/remove-line-item",
  )
  fun removeLineItem(
    @Param("cartId") cartId: UUID,
    command: WarehouseRemoveLineItem,
  ): Any
}

data class WarehouseRemoveLineItem(val lineItemId: UUID)

@Configuration
class WarehouseClientConfiguration {

  @Bean
  fun warehouseClient(objectMapper: ObjectMapper): WarehouseClient {
    return Feign.builder()
      .encoder(JacksonEncoder(objectMapper))
      .decoder(JacksonDecoder(objectMapper))
      .target(WarehouseClient::class.java, "http://localhost:8081")
  }
}

package com.pintomau.temporalspring.shipping.features

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.pintomau.temporalspring.shipping.domain.WarehouseCart
import com.pintomau.temporalspring.shipping.domain.WarehouseCartRepository
import java.util.*
import org.springframework.stereotype.Service
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class AddLineItemController(
  private val addLineItemHandler: AddLineItemHandler,
) {

  @PostMapping("carts/{id}/add-line-item")
  fun addLineItem(
    @PathVariable("id") cartId: UUID,
    @Validated @RequestBody command: AddLineItemCommand,
  ): WarehouseCart {
    return addLineItemHandler.handle(cartId, command)
  }
}

data class AddLineItemCommand
@JsonCreator
constructor(
  @JsonProperty("version") val version: Int,
  @JsonProperty("productId") val productId: UUID,
  @JsonProperty("requestId") val requestId: String,
)

@Service
class AddLineItemHandler(
  private val warehouseCartRepository: WarehouseCartRepository,
) {

  fun handle(cartId: UUID, command: AddLineItemCommand): WarehouseCart {
    val cart = warehouseCartRepository.getOrCreateById(cartId)
    cart.addLineItem(command.productId, command.requestId)
    warehouseCartRepository.replace(cart, command.requestId)
    return cart
  }
}

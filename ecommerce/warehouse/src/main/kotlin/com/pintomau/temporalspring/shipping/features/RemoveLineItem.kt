package com.pintomau.temporalspring.shipping.features

import com.pintomau.temporalspring.shipping.domain.WarehouseCart
import com.pintomau.temporalspring.shipping.domain.WarehouseCartRepository
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class RemoveLineItemController(
  private val removeLineItemHandler: RemoveLineItemHandler,
) {

  @PostMapping("carts/{id}/remove-line-item")
  fun removeLineItem(
    @PathVariable("id") id: UUID,
    @Validated @RequestBody command: RemoveLineItemCommand,
  ): WarehouseCart {
    return removeLineItemHandler.handle(id, command)
  }
}

data class RemoveLineItemCommand(val lineItemId: UUID)

@Service
class RemoveLineItemHandler(
  private val warehouseCartRepository: WarehouseCartRepository,
) {

  fun handle(cartId: UUID, command: RemoveLineItemCommand): WarehouseCart {
    val cart = warehouseCartRepository.getById(cartId)
    cart.removeLineItem(command.lineItemId)
    return warehouseCartRepository.replace(cart)
  }
}

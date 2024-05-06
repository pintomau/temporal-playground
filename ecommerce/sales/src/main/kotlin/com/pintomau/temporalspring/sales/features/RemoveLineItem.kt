package com.pintomau.temporalspring.sales.features

import com.pintomau.temporalspring.sales.core.SalesCart
import com.pintomau.temporalspring.sales.core.SalesCartRepository
import java.util.*
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

  @PostMapping("{cartId}/remove-line-item")
  fun removeLineItem(
    @PathVariable("cartId") cartId: UUID,
    @Validated @RequestBody removeLineItem: RemoveLineItem,
  ): SalesCart {
    return removeLineItemHandler.handle(cartId, removeLineItem)
  }
}

data class RemoveLineItem(val version: Int, val lineItemId: UUID)

@Service
class RemoveLineItemHandler(
  private val salesCartRepository: SalesCartRepository,
) {

  fun handle(cartId: UUID, removeLineItem: RemoveLineItem): SalesCart {
    val cart = salesCartRepository.getByIdAndVersion(cartId, removeLineItem.version)
    cart.removeLineItem(removeLineItem.lineItemId)
    return salesCartRepository.replace(cart, removeLineItem.version)
  }
}

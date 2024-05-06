package com.pintomau.temporalspring.marketing.features

import com.pintomau.temporalspring.marketing.domain.MarketingCart
import com.pintomau.temporalspring.marketing.domain.MarketingCartRepository
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

  @PostMapping("carts/{id}/remove-line-item")
  fun removeLineItem(
    @PathVariable("id") id: UUID,
    @Validated @RequestBody command: RemoveLineItemCommand,
  ): MarketingCart {
    return removeLineItemHandler.handle(id, command)
  }
}

data class RemoveLineItemCommand(val lineItemId: UUID)

@Service
class RemoveLineItemHandler(
  private val marketingCartRepository: MarketingCartRepository,
) {

  fun handle(cartId: UUID, command: RemoveLineItemCommand): MarketingCart {
    val cart = marketingCartRepository.getById(cartId)
    cart.removeLineItem(command.lineItemId)
    return marketingCartRepository.replace(cart)
  }
}

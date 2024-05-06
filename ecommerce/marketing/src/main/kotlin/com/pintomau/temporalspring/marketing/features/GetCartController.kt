package com.pintomau.temporalspring.marketing.features

import com.pintomau.temporalspring.marketing.domain.MarketingCart
import com.pintomau.temporalspring.marketing.domain.MarketingCartRepository
import java.util.*
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
class GetCartController(
  private val warehouseCartRepository: MarketingCartRepository,
) {

  @GetMapping("carts/{id}")
  fun getCart(@PathVariable("id") cartId: UUID): MarketingCart {
    return warehouseCartRepository.getById(cartId)
  }
}

package com.pintomau.temporalspring.shipping.features

import com.pintomau.temporalspring.shipping.domain.WarehouseCart
import com.pintomau.temporalspring.shipping.domain.WarehouseCartRepository
import java.util.UUID
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
class GetCartController(
  private val warehouseCartRepository: WarehouseCartRepository,
) {

  @GetMapping("carts/{id}")
  fun getCart(@PathVariable("id") cartId: UUID): WarehouseCart {
    return warehouseCartRepository.getById(cartId)
  }
}

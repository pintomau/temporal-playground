package com.pintomau.temporalspring.shipping.domain

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.*
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.NOT_FOUND)
class WarehouseCartNotFoundException(@JsonProperty val cartId: UUID) :
  RuntimeException("Cart '$cartId' not found!")

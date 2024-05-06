package com.pintomau.temporalspring.sales.core

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.*
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.NOT_FOUND)
class SalesCartNotFoundException(@JsonProperty val cartId: UUID) :
  RuntimeException("Cart '$cartId' not found!")

@ResponseStatus(HttpStatus.CONFLICT)
class SalesCartVersionMismatchException(
  @JsonProperty val requestedVersion: Int,
  @JsonProperty val actualVersion: Int,
) : RuntimeException("Requested Cart version $requestedVersion. Actual version is $actualVersion")

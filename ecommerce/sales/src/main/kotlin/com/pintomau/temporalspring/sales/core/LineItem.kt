package com.pintomau.temporalspring.sales.core

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import java.util.*

data class LineItem
@JsonCreator
constructor(
  @JsonProperty("lineItemId") val lineItemId: UUID,
  @JsonProperty("productId") val productId: UUID,
)

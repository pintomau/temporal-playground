package com.pintomau.temporalspring.sales.core

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.util.*

@JsonTypeInfo(
  use = JsonTypeInfo.Id.NAME,
  include = JsonTypeInfo.As.EXISTING_PROPERTY,
  property = "type",
  visible = true,
)
@JsonSubTypes(
  value =
    [
      Type(value = Event.AddLineItemStarted::class, name = Event.AddLineItemStarted.TYPE),
      Type(value = Event.LineItemAdded::class, name = Event.LineItemAdded.TYPE),
    ],
)
sealed interface Event {

  val type: String

  data class AddLineItemStarted(
    val cartId: UUID,
    val version: Int,
    val requestId: String,
    val productId: UUID,
    val lineItemId: UUID,
  ) : Event {
    companion object {
      internal const val TYPE = "AddLineItemStarted"
    }

    override val type = TYPE
  }

  data class LineItemAdded(
    val cartId: UUID,
    val version: Int,
    val requestId: String,
    val productId: UUID,
    val lineItemId: UUID,
  ) : Event {
    companion object {
      internal const val TYPE = "LineItemAdded"
    }

    override val type = TYPE
  }
}

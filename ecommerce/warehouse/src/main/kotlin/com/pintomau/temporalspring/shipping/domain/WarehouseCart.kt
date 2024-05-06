package com.pintomau.temporalspring.shipping.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import java.util.UUID
import org.springframework.data.annotation.Id

private const val REQUEST_IDS = 9

class WarehouseCart(
  @Id val id: UUID,
  private val _lineItems: MutableList<LineItem> = mutableListOf(),
  @JsonIgnore val requestIds: ArrayDeque<String> = ArrayDeque(REQUEST_IDS + 1),
) {

  /**
   * Provide public read-only access to [_lineItems].
   *
   * Unfortunately, regular
   * [backing properties](https://kotlinlang.org/docs/properties.html#backing-properties) doesn't
   * play well with MongoDB's deserialization
   */
  @get:JsonProperty("lineItems")
  val lineItems: List<LineItem>
    get() = _lineItems

  fun addLineItem(productId: UUID, requestId: String) {
    _lineItems.add(LineItem(UUID.randomUUID(), productId))
    appendRequest(requestId)
  }

  fun removeLineItem(lineItemId: UUID) {
    _lineItems.removeIf { it.lineItemId == lineItemId }
  }

  private fun appendRequest(requestId: String) {
    if (requestIds.size == REQUEST_IDS) {
      requestIds.removeLast()
    }

    requestIds.addFirst(requestId)
  }

  override fun toString(): String {
    return "WarehouseCart(id=$id, _lineItems=$_lineItems, requestIds=$requestIds)"
  }
}

data class LineItem(val lineItemId: UUID, val productId: UUID)

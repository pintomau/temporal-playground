package com.pintomau.temporalspring.sales.core

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import java.util.*
import kotlin.collections.ArrayDeque
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.mongodb.core.mapping.Field

const val SALES_CART_PROCESSING_QUEUE = "sales-cart"

private const val REQUEST_IDS = 9

class SalesCart(
  @JsonProperty("id") @Id val id: UUID = UUID.randomUUID(),
  @JsonProperty("version") version: Int = 0,
  @JsonProperty("lineItems")
  @Field("lineItems")
  private val _lineItems: MutableList<LineItem> = mutableListOf(),
  @JsonProperty("requestIds")
  @get:JsonIgnore
  val requestIds: ArrayDeque<String> = ArrayDeque(REQUEST_IDS + 1),
) {

  @Version
  var version: Int = version
    private set

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

  fun addLineItem(lineItem: LineItem, requestId: String) {
    _lineItems.add(lineItem)
    appendRequest(requestId)
    updateVersion()
  }

  fun removeLineItem(lineItemId: UUID) {
    val removed = _lineItems.removeIf { lineItemId == it.lineItemId }

    if (removed) {
      updateVersion()
    }
  }

  private fun appendRequest(requestId: String) {
    if (requestIds.size == REQUEST_IDS) {
      requestIds.removeLast()
    }

    requestIds.addFirst(requestId)
  }

  private fun updateVersion() {
    version++
  }

  override fun toString(): String {
    return "SalesCart(id=$id, _lineItems=$_lineItems, requestIds=$requestIds, version=$version)"
  }
}

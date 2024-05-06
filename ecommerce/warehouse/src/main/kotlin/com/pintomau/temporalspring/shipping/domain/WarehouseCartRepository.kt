package com.pintomau.temporalspring.shipping.domain

import java.util.*
import org.springframework.data.mongodb.core.*
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Repository

@Repository
class WarehouseCartRepository(
  private val mongoTemplate: MongoTemplate,
) {

  fun getById(id: UUID): WarehouseCart {
    return mongoTemplate.findById<WarehouseCart>(id) ?: throw WarehouseCartNotFoundException(id)
  }

  fun getOrCreateById(cartId: UUID): WarehouseCart {
    val query = Query(Criteria.where("_id").`is`(cartId))
    val update = Update().unset("noop")
    return mongoTemplate.findAndModify<WarehouseCart>(
      query,
      update,
      FindAndModifyOptions().upsert(true).returnNew(true),
    ) ?: throw RuntimeException("Could not find or create WarehouseCart with id $cartId")
  }

  fun replace(cart: WarehouseCart, requestId: String? = null): WarehouseCart {
    val query =
      Query(
        Criteria.where("_id").`is`(cart.id).apply {
          if (null != requestId) {
            and("requestIds").ne(requestId)
          }
        },
      )

    mongoTemplate.update<WarehouseCart>().matching(query).replaceWith(cart).replaceFirst()

    return cart
  }
}

package com.pintomau.temporalspring.marketing.domain

import java.util.*
import org.springframework.data.mongodb.core.*
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Repository

@Repository
class MarketingCartRepository(
  private val mongoTemplate: MongoTemplate,
) {

  fun getById(id: UUID): MarketingCart {
    return mongoTemplate.findById<MarketingCart>(id) ?: throw MarketingCartNotFoundException(id)
  }

  fun getOrCreateById(cartId: UUID): MarketingCart {
    val query = Query(Criteria.where("_id").`is`(cartId))
    val update = Update().unset("noop")
    return mongoTemplate.findAndModify<MarketingCart>(
      query,
      update,
      FindAndModifyOptions().upsert(true).returnNew(true),
    ) ?: throw RuntimeException("Could not find or create MarketingCart with id $cartId")
  }

  fun replace(cart: MarketingCart, requestId: String? = null): MarketingCart {
    val query =
      Query(
        Criteria.where("_id").`is`(cart.id).apply {
          if (null != requestId) {
            and("requestIds").ne(requestId)
          }
        },
      )

    mongoTemplate.update<MarketingCart>().matching(query).replaceWith(cart).replaceFirst()

    return cart
  }
}

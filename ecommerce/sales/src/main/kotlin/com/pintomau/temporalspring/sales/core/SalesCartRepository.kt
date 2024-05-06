package com.pintomau.temporalspring.sales.core

import java.util.*
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.*
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Repository

@Repository
class SalesCartRepository(
  private val mongoTemplate: MongoTemplate,
) {

  fun create(salesCart: SalesCart): SalesCart {
    return mongoTemplate.save(salesCart)
  }

  fun getById(id: UUID): SalesCart {
    return mongoTemplate.findById<SalesCart>(id) ?: throw SalesCartNotFoundException(id)
  }

  fun getByIdForUpdate(id: UUID): SalesCart {
    val query =
      Query(
        Criteria.where("_id").`is`(id),
      )

    // https://www.mongodb.com/blog/post/how-to-select--for-update-inside-mongodb-transactions
    val mongoLock = MongoLock()
    return mongoTemplate.findAndModify(
      query,
      Update().set("lock", mongoLock),
      SalesCart::class.java,
    ) ?: throw SalesCartNotFoundException(id)
  }

  data class MongoLock(val lockNumber: ObjectId = ObjectId())

  fun getByIdAndVersion(id: UUID, version: Int): SalesCart {
    val cart = getById(id)
    if (version != cart.version) {
      throw SalesCartVersionMismatchException(version, cart.version)
    }

    return cart
  }

  fun replace(cart: SalesCart): SalesCart {
    return replace(cart, null, null)
  }

  fun replace(cart: SalesCart, version: Int): SalesCart {
    return replace(cart, version, null)
  }

  fun replace(cart: SalesCart, version: Int?, requestId: String?): SalesCart {
    val query =
      Query(
        Criteria.where("_id")
          .`is`(cart.id)
          .apply {
            if (version != null) {
              and("version").`is`(version)
            }
          }
          .apply {
            if (null != requestId) {
              and("requestIds").ne(requestId)
            }
          },
      )

    val result = mongoTemplate.update<SalesCart>().matching(query).replaceWith(cart).replaceFirst()

    if (result.modifiedCount != 1L) {
      throw RuntimeException("Couldn't update cart '$cart'")
    }

    return cart
  }

  fun update(cart: SalesCart, update: Update): SalesCart {
    val query =
      Query(
        Criteria.where("_id").`is`(cart.id),
      )

    return mongoTemplate
      .update<SalesCart>()
      .matching(query)
      .apply(update)
      .withOptions(FindAndModifyOptions().returnNew(true).upsert(false))
      .findAndModifyValue() ?: cart
  }

  fun update(cart: SalesCart, update: Update, requestId: String, version: Int): SalesCart {
    val query =
      Query(
        Criteria.where("_id")
          .`is`(cart.id)
          .and("requestIds")
          .ne(requestId)
          .and("version")
          .`is`(version),
      )

    return mongoTemplate
      .update<SalesCart>()
      .matching(query)
      .apply(update)
      .withOptions(FindAndModifyOptions().returnNew(true).upsert(false))
      .findAndModifyValue() ?: cart
  }
}

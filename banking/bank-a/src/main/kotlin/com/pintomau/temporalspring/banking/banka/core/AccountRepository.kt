package com.pintomau.temporalspring.banking.banka.core

import java.util.*
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.findOne
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Repository

internal const val REQUEST_IDS = 9

/**
 * Appends the given [requestId] to [Account.requestIds] to enforce idempotency at persistence
 * level. This is just one trivial and maybe naïve way to implement idempotency.
 *
 * We're adding this here, because, otherwise, we'd need to pass the request id to every method of
 * Account we want to make idempotent.
 */
private fun Account.appendRequestId(requestId: String) {
  if (requestIds.size == REQUEST_IDS) {
    requestIds.removeLast()
  }

  requestIds.addFirst(requestId)
}

@Repository
class AccountRepository(
  private val template: MongoTemplate,
) {

  fun create(account: Account): Account {
    return template.save(account)
  }

  fun getById(id: UUID): Account {
    val query = Query(Criteria.where("_id").`is`(id))
    return template.findOne<Account>(query) ?: throw AccountNotFound(id)
  }

  fun replace(account: Account, requestId: String): Account {
    account.appendRequestId(requestId)

    val query =
      Query(
        Criteria.where("_id").`is`(account.id).and("requestIds").ne(requestId),
      )

    val r = template.replace<Account>(query, account)
    if (r.modifiedCount == 0L) {
      // in case the request was already processed, request the latest version
      // using transactions is probably more elegant, but this works as well.
      return template.findOne<Account>(Query(Criteria.where("_id").`is`(account.id)))
        ?: throw AccountNotFound(account.id)
    }

    return account
  }
}

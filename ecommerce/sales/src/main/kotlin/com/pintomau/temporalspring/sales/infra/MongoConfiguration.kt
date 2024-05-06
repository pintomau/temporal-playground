package com.pintomau.temporalspring.sales.infra

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.MongoDatabaseFactory
import org.springframework.data.mongodb.MongoTransactionManager

@Configuration
class MongoConfiguration {

  // https://docs.spring.io/spring-data/mongodb/reference/mongodb/client-session-transactions.html#mongo.transactions.reactive-tx-manager
  @Bean
  fun transactionManager(mongoDatabaseFactory: MongoDatabaseFactory): MongoTransactionManager {
    return MongoTransactionManager(mongoDatabaseFactory)
  }
}

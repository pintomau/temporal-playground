package com.pintomau.temporalspring.sales.infra

import com.fasterxml.jackson.databind.ObjectMapper
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import org.springframework.beans.factory.InitializingBean
import org.springframework.context.annotation.Configuration

object JsonUtils {

  var objectMapper: ObjectMapper = ObjectMapper()

  @OptIn(ExperimentalContracts::class)
  fun <T> withMapper(with: ObjectMapper.() -> T): T {
    contract { callsInPlace(with, InvocationKind.EXACTLY_ONCE) }

    return with(objectMapper)
  }
}

@Configuration
class ObjectMapperConfiguration(private val objectMapper: ObjectMapper) : InitializingBean {

  override fun afterPropertiesSet() {
    JsonUtils.objectMapper = objectMapper
  }
}

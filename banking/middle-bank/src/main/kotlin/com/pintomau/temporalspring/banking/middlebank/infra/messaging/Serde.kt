package com.pintomau.temporalspring.banking.middlebank.infra.messaging

import com.pintomau.temporalspring.banking.middlebank.infra.JsonUtils
import org.springframework.amqp.core.MessageBuilder

fun objectMessageBuilder(message: Any): MessageBuilder {
  val body = JsonUtils.withMapper { writeValueAsBytes(message) }
  return MessageBuilder.withBody(body)
}

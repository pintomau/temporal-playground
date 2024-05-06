@file:Suppress("ktlint:standard:filename")

package com.pintomau.temporalspring.sales.features

import com.pintomau.temporalspring.sales.core.Event
import com.pintomau.temporalspring.sales.infra.sse.SseSubscriberRegistrar
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.messaging.Message
import org.springframework.stereotype.Component
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

private const val MOCK_USER = "123"
private val logger = KotlinLogging.logger {}

@Component
class SseCartEventNotifier(
  private val sseSubscriberRegistrar: SseSubscriberRegistrar,
) {

  @RabbitListener(queues = ["receiver.sse"])
  fun listen(msg: Message<Event>) {
    val sseMsg =
      when (val payload = msg.payload) {
        is Event.AddLineItemStarted -> {
          logger.info { "Add line item started" }
          SseEmitter.event().name(payload.type).data(payload)
        }
        is Event.LineItemAdded -> {
          logger.info { "Line item added" }
          SseEmitter.event().name(payload.type).data(payload)
        }
      }

    sseSubscriberRegistrar.sendEvent(MOCK_USER, sseMsg)
  }
}

// Listener per event type is also supported
// @Component
// class AddLineItemStartedListener(
//  private val sseSubscriberRegistrar: SseSubscriberRegistrar,
// ) {
//
//  @RabbitListener(queues = ["receiver.sse"])
//  fun listen(msg: Message<Event.AddLineItemStarted>) {
//    val sseMsg = SseEmitter.event().name("AddLineItemStarted").data(msg.payload)
//
//    sseSubscriberRegistrar.sendEvent(MOCK_USER, sseMsg)
//  }
// }
//
//
// @Component
// class LineItemAddedListener(
//  private val sseSubscriberRegistrar: SseSubscriberRegistrar,
// ) {
//
//  @RabbitListener(queues = ["receiver.sse"])
//  fun listen(msg: Message<Event.LineItemAdded>) {
//    val sseMsg = SseEmitter.event().name("LineItemAdded").data(msg.payload)
//
//    sseSubscriberRegistrar.sendEvent(MOCK_USER, sseMsg)
//  }
// }

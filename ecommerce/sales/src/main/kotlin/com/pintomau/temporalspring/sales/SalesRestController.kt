package com.pintomau.temporalspring.sales

import com.pintomau.temporalspring.sales.core.SalesCart
import com.pintomau.temporalspring.sales.core.SalesCartRepository
import com.pintomau.temporalspring.sales.infra.sse.SseSubscriberRegistrar
import java.util.*
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@RestController
class SalesRestController(
  private val sseSubscriberRegistrar: SseSubscriberRegistrar,
  private val salesCartRepository: SalesCartRepository,
) {

  @GetMapping("cart/{cartId}")
  fun getCart(@PathVariable("cartId") cartId: UUID): SalesCart {
    return salesCartRepository.getById(cartId)
  }

  @PostMapping("cart")
  fun createCart(): SalesCart {
    return salesCartRepository.create(salesCart = SalesCart(UUID.randomUUID()))
  }

  @CrossOrigin(origins = ["http://localhost:8080", "https://unpkg.com"])
  @GetMapping("subscribe/{userId}", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
  fun subscribe(@PathVariable("userId") userId: String): SseEmitter {
    logger.info { "registering subscription to $userId" }
    return sseSubscriberRegistrar
      .findOrPutEmitter(userId) {
        SseEmitter(60_000L).apply {
          onCompletion { sseSubscriberRegistrar.removeEmitter(userId) }
          onTimeout {
            complete()
            sseSubscriberRegistrar.removeEmitter(userId)
          }
        }
      }
      .also { logger.info { "subscribed for $userId" } }
  }

  @PostMapping(
    "send-message/{userId}",
    consumes = [MediaType.APPLICATION_JSON_VALUE],
    produces = [MediaType.APPLICATION_JSON_VALUE],
  )
  fun sendMessage(@PathVariable("userId") userId: String, @RequestBody message: SseMessage) {
    val event = SseEmitter.event().name(message.name).data(message.data)
    sseSubscriberRegistrar.sendEvent(userId, event)
  }
}

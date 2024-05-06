package com.pintomau.temporalspring.sales.infra.sse

import java.util.concurrent.ConcurrentHashMap
import org.springframework.stereotype.Service
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter.SseEventBuilder

@Service
class SseSubscriberRegistrar(
  private val userToEmitter: ConcurrentHashMap<String, SseEmitter> = ConcurrentHashMap(),
) {

  fun findOrPutEmitter(userId: String, put: () -> SseEmitter): SseEmitter {
    return userToEmitter.getOrPut(userId, put)
  }

  fun removeEmitter(userId: String) {
    userToEmitter.remove(userId)
  }

  fun sendEvent(userId: String, message: SseEventBuilder) {
    userToEmitter[userId]?.send(message)
  }
}

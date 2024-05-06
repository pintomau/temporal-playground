package com.pintomau.temporalspring.sales.infra

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.FanoutExchange
import org.springframework.amqp.core.MessageBuilder
import org.springframework.amqp.core.Queue
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.beans.factory.InitializingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

fun objectMessageBuilder(message: Any): MessageBuilder {
  val body = JsonUtils.withMapper { writeValueAsBytes(message) }
  return MessageBuilder.withBody(body)
}

@Configuration
class RabbitConfig {

  // deserializes received messages
  @Bean
  fun jackson2JsonMessageConverter(objectMapper: ObjectMapper): Jackson2JsonMessageConverter {
    return Jackson2JsonMessageConverter(objectMapper)
  }

  @Configuration
  class ProducerConfig(
    private val rabbitTemplate: RabbitTemplate,
    private val jackson2JsonMessageConverter: Jackson2JsonMessageConverter,
  ) : InitializingBean {

    override fun afterPropertiesSet() {
      // serializes sent messages
      rabbitTemplate.messageConverter = jackson2JsonMessageConverter
    }

    @Bean fun cartsFanout() = FanoutExchange("carts.fanout")
  }

  @Configuration
  class SseListener(
    private val cartsFanout: FanoutExchange,
  ) {

    @Bean fun sseQueue() = Queue("receiver.sse")

    @Bean
    fun sseQueueCartsBinding(
      cartsFanout: FanoutExchange,
      sseQueue: Queue,
    ) = BindingBuilder.bind(sseQueue).to(cartsFanout)
  }
}

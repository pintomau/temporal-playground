package com.pintomau.temporalspring.banking.middlebank.infra.messaging

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.amqp.core.*
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.beans.factory.InitializingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

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

    @Bean fun transferFanout() = FanoutExchange("transfer.fanout")
  }

  // region MailQueue
  @Bean fun mailQueue() = Queue("receiver.mail_queue")

  @Bean
  fun mailQueueFanoutBinding(
    fanoutExchange: FanoutExchange,
    mailQueue: Queue,
  ) = BindingBuilder.bind(mailQueue).to(fanoutExchange)
  // endregion

  // region PdfQueue
  @Bean fun pdfQueue() = Queue("receiver.pdf_queue")

  @Bean
  fun pdfQueueFanoutBinding(
    fanoutExchange: FanoutExchange,
    pdfQueue: Queue,
  ) = BindingBuilder.bind(pdfQueue).to(fanoutExchange)
  // endregion

  // region Transfer Report
  @Bean fun transferReportQueue() = Queue("receiver.transfer_report_queue")

  @Bean
  fun transferReportQueueFanoutBinding(
    fanoutExchange: FanoutExchange,
    transferReportQueue: Queue,
  ) = BindingBuilder.bind(transferReportQueue).to(fanoutExchange)
  // endregion
}

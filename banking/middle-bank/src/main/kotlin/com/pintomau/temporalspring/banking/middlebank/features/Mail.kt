@file:Suppress("ktlint:standard:filename")

package com.pintomau.temporalspring.banking.middlebank.features

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.messaging.Message
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
class MailTransferFinishedListener {

  @RabbitListener(queues = ["receiver.mail_queue"])
  fun pdfListener(transferFinished: Message<TransferFinished>) {
    logger.info { "generating mail for: $transferFinished" }
  }
}

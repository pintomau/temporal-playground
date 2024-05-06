package com.pintomau.temporalspring.sales

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*

val logger = KotlinLogging.logger {}

@Controller
@RequestMapping("app")
@CrossOrigin(origins = ["http://localhost:8080", "https://unpkg.com"])
class SalesController {

  @GetMapping fun index() = "index"
}

data class SseMessage(val name: String, val data: String)

data class BuyModel(val sku: String, val requestId: String)

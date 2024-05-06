package com.pintomau.temporalspring.shipping

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication class WarehouseApplication

fun main(args: Array<String>) {
  runApplication<WarehouseApplication>(*args)
}

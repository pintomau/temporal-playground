package com.pintomau.temporalspring.banking.middlebank

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication class MiddleBankApplication

fun main(args: Array<String>) {
  runApplication<MiddleBankApplication>(*args)
}

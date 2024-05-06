package com.pintomau.temporalspring.banking.banka.core

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.*
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.NOT_FOUND)
class AccountNotFound(@JsonProperty val id: UUID) : RuntimeException("Account '$id' not found!")

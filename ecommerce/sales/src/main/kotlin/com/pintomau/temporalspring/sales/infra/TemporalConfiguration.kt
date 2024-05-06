package com.pintomau.temporalspring.sales.infra

import com.fasterxml.jackson.databind.ObjectMapper
import io.temporal.common.converter.DefaultDataConverter
import io.temporal.common.converter.GlobalDataConverter
import io.temporal.common.converter.JacksonJsonPayloadConverter
import org.springframework.beans.factory.InitializingBean
import org.springframework.context.annotation.Configuration

@Configuration
class TemporalConfiguration(
  private val objectMapper: ObjectMapper,
) : InitializingBean {

  override fun afterPropertiesSet() {
    configureDataConverter()
  }

  /** Override default Temporal Jackson Configuration with Spring's */
  private fun configureDataConverter() {
    val dataConverter =
      DefaultDataConverter.newDefaultInstance()
        .withPayloadConverterOverrides(
          JacksonJsonPayloadConverter(objectMapper),
        )

    GlobalDataConverter.register(dataConverter)
  }
}

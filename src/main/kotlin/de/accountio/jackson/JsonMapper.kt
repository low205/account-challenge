package de.accountio.jackson

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

object JsonMapper {
    val mapper: ObjectMapper = jacksonObjectMapper()

    init {
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true)
        mapper.registerModule(JavaTimeModule())
    }
}

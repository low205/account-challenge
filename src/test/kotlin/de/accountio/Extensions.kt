package de.accountio

import com.fasterxml.jackson.module.kotlin.readValue
import de.accountio.jackson.JsonMapper
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.TestApplicationResponse
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication
import kotlinx.coroutines.channels.SendChannel
import kotlin.test.assertNotNull

fun testApp(block: TestApplicationEngine.() -> Unit) {
    withTestApplication({ module(testing = true) }) {
        block
    }
}

fun TestApplicationEngine.callGet(uri: String): TestApplicationResponse {
    return handleRequest(HttpMethod.Get, uri).response
}

fun TestApplicationEngine.callDelete(uri: String): TestApplicationResponse {
    return handleRequest(HttpMethod.Delete, uri).response
}

fun TestApplicationEngine.callPost(uri: String, body: Any? = null): TestApplicationResponse {
    return when (body) {
        null -> handleRequest(HttpMethod.Post, uri).response
        else -> handleRequest(HttpMethod.Post, "/accounts") {
            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(JsonMapper.mapper.writeValueAsString(body))
        }.response
    }
}

fun <R> TestApplicationResponse.with(block: TestApplicationResponse.() -> R): R {
    return block(this)
}

inline fun <reified T> TestApplicationResponse.responseBody(): T = JsonMapper.mapper.readValue(assertNotNull(content))

inline fun <E> SendChannel<E>.use(block: (SendChannel<E>) -> Unit) {
    try {
        block(this)
        this.close()
    } catch (e: Throwable) {
        this.close(e)
    }
}

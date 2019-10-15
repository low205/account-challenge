package de.accountio

import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ApplicationTest {
    @Test
    fun testRoot() {
        withTestApplication({ module(testing = true) }) {
            handleRequest(HttpMethod.Get, "/").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                val content = assertNotNull(response.content)
                assertEquals(
                    mapOf("name" to "Accounting application", "version" to "0.0.1"),
                    mapper.readValue(content)
                )
            }
        }
    }

    @Test
    fun defaultPathShouldReturnNotFound() {
        withTestApplication({ module(testing = true) }) {
            handleRequest(HttpMethod.Get, "/Some/Random/Path").apply {
                assertEquals(HttpStatusCode.NotFound, response.status())
                val content = assertNotNull(response.content)
                assertEquals(
                    mapOf("path" to "/Some/Random/Path"),
                    mapper.readValue(content)
                )
            }
        }
    }
}
package de.accountio

import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {
    @Test
    fun testRoot() = testApp {
        callGet("/").with {
            assertEquals(HttpStatusCode.OK, status())
            assertEquals(mapOf("name" to "Accounting application", "version" to "0.0.1"), responseBody())
        }
    }

    @Test
    fun defaultPathShouldReturnNotFound() = testApp {
        callGet("/Some/Random/Path").with {
            assertEquals(HttpStatusCode.NotFound, status())
            assertEquals(mapOf("path" to "/Some/Random/Path"), responseBody())
        }
    }

    @Test
    fun randomErrorShouldReturnServerError() = testApp {
        callGet("/throw").with {
            assertEquals(HttpStatusCode.InternalServerError, status())
            assertEquals(mapOf("error" to "throw random error"), responseBody())
        }
    }
}

package de.accountio.web

import com.fasterxml.jackson.module.kotlin.readValue
import de.accountio.domain.Account
import de.accountio.domain.AccountStatuses
import de.accountio.mapper
import de.accountio.module
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.contentType
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AccountResourceKtTest {
    @Test
    fun shouldCreateNewAccount() {
        withTestApplication({ module(testing = true) }) {
            handleRequest(HttpMethod.Post, "/accounts").apply {
                assertEquals(HttpStatusCode.Created, response.status())
                assertTrue(response.contentType().match(ContentType.Application.Json))
                val account = mapper.readValue<Account>(assertNotNull(response.content))
                assertTrue(account.id > 0)
                assertTrue(account.number.isNotBlank())
                assertEquals(account.status, AccountStatuses.OPEN)
            }
        }
    }

    @Test
    fun shouldBadRequestWhenFindingAccountByInvalidId() {
        withTestApplication({ module(testing = true) }) {
            handleRequest(HttpMethod.Get, "/accounts/invalidId").apply {
                assertEquals(HttpStatusCode.BadRequest, response.status())
                assertTrue(response.contentType().match(ContentType.Application.Json))
                val response = mapper.readValue<Map<String, String>>(assertNotNull(response.content))
                assertEquals(response["error"], "Must provide id")
            }
        }
    }

    @Test
    fun shouldNotFoundWhenAccountNotFound() {
        withTestApplication({ module(testing = true) }) {
            handleRequest(HttpMethod.Get, "/accounts/10").apply {
                assertEquals(HttpStatusCode.NotFound, response.status())
                val response = mapper.readValue<Map<String, Int>>(assertNotNull(response.content))
                assertEquals(response["id"], 10)
            }
        }
    }
}

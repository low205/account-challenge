package de.accountio.web

import com.fasterxml.jackson.module.kotlin.readValue
import de.accountio.domain.Account
import de.accountio.domain.AccountStatuses
import de.accountio.domain.TransactionType
import de.accountio.jackson.JsonMapper.mapper
import de.accountio.module
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.contentType
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AccountResourceKtTest {
    @Test
    fun shouldCreateNewAccount() {
        withTestApplication({ module() }) {
            val createdAccount = with(handleRequest(HttpMethod.Post, "/accounts")) {
                assertEquals(HttpStatusCode.Created, response.status())
                assertTrue(response.contentType().match(ContentType.Application.Json))
                val account = mapper.readValue<Account>(assertNotNull(response.content))
                assertTrue(account.id > 0)
                assertTrue(account.number.isNotBlank())
                assertEquals(account.status, AccountStatuses.OPEN)
                account
            }
            handleRequest(HttpMethod.Get, "/accounts/${createdAccount.id}").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertTrue(response.contentType().match(ContentType.Application.Json))
                val account = mapper.readValue<Account>(assertNotNull(response.content))
                assertEquals(createdAccount, account)
            }
        }
    }

    @Test
    fun shouldCreateNewAccountWithInitialBalance() = withTestApplication({ module() }) {
        val accountId = with(handleRequest(HttpMethod.Post, "/accounts") {
            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(mapper.writeValueAsString(CreateAccountRequest(100.toBigDecimal())))
        }) {
            assertEquals(HttpStatusCode.Created, response.status())
            assertTrue(response.contentType().match(ContentType.Application.Json))
            val account = mapper.readValue<Account>(assertNotNull(response.content))
            assertTrue(account.id > 0)
            assertTrue(account.number.isNotBlank())
            assertEquals(account.status, AccountStatuses.OPEN)
            account.id
        }
        with(handleRequest(HttpMethod.Get, "/accounts/$accountId/balance")) {
            assertEquals(HttpStatusCode.OK, response.status())
            assertTrue(response.contentType().match(ContentType.Application.Json))
            val balanceResponse = mapper.readValue<AccountBalanceResponse>(assertNotNull(response.content))
            assertEquals(100.toBigDecimal(), balanceResponse.balance)
        }
    }

    @Test
    fun shouldBadRequestWhenFindingAccountByInvalidId() {
        withTestApplication({ module() }) {
            handleRequest(HttpMethod.Get, "/accounts/invalidId").apply {
                assertEquals(HttpStatusCode.BadRequest, response.status())
                assertTrue(response.contentType().match(ContentType.Application.Json))
                val response = mapper.readValue<Map<String, String>>(assertNotNull(response.content))
                assertEquals(response["error"], "Must provide id")
            }
        }
    }

    @Test
    fun shouldNotFoundWhenAccountNotFoundOnFindById() {
        withTestApplication({ module() }) {
            handleRequest(HttpMethod.Get, "/accounts/10").apply {
                assertEquals(HttpStatusCode.NotFound, response.status())
                val response = mapper.readValue<Map<String, Int>>(assertNotNull(response.content))
                assertEquals(response["id"], 10)
            }
        }
    }

    @Test
    fun shouldNotFoundWhenAccountNotFoundOnDelete() {
        withTestApplication({ module() }) {
            handleRequest(HttpMethod.Delete, "/accounts/10").apply {
                assertEquals(HttpStatusCode.NotFound, response.status())
                val response = mapper.readValue<Map<String, Int>>(assertNotNull(response.content))
                assertEquals(response["id"], 10)
            }
        }
    }

    @Test
    fun shouldBadRequestWhenDeleteAccountByInvalidId() {
        withTestApplication({ module() }) {
            handleRequest(HttpMethod.Delete, "/accounts/invalidId").apply {
                assertEquals(HttpStatusCode.BadRequest, response.status())
                assertTrue(response.contentType().match(ContentType.Application.Json))
                val response = mapper.readValue<Map<String, String>>(assertNotNull(response.content))
                assertEquals(response["error"], "Must provide id")
            }
        }
    }

    @Test
    fun shouldReturnClosedAccountOnDelete() {
        withTestApplication({ module() }) {
            handleRequest(HttpMethod.Post, "/accounts").apply {
                assertEquals(HttpStatusCode.Created, response.status())
                assertTrue(response.contentType().match(ContentType.Application.Json))
                val account = mapper.readValue<Account>(assertNotNull(response.content))
                assertTrue(account.id > 0)
                assertTrue(account.number.isNotBlank())
                assertEquals(AccountStatuses.OPEN, account.status)

                handleRequest(HttpMethod.Delete, "/accounts/${account.id}").apply {
                    assertEquals(HttpStatusCode.Accepted, response.status())
                    val deleted = mapper.readValue<Account>(assertNotNull(response.content))
                    assertEquals(AccountStatuses.CLOSED, deleted.status)
                    assertEquals(account.id, deleted.id)
                    assertEquals(account.number, deleted.number)
                }
            }
        }
    }

    @Test
    fun shouldCreateTransfer() {
        withTestApplication({ module() }) {
            val accountOne = with(handleRequest(HttpMethod.Post, "/accounts") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(mapper.writeValueAsString(CreateAccountRequest(100.toBigDecimal())))
            }) {
                assertEquals(HttpStatusCode.Created, response.status())
                assertTrue(response.contentType().match(ContentType.Application.Json))
                val account = mapper.readValue<Account>(assertNotNull(response.content))
                assertTrue(account.id > 0)
                assertTrue(account.number.isNotBlank())
                assertEquals(AccountStatuses.OPEN, account.status)
                account
            }
            val accountTwo = with(handleRequest(HttpMethod.Post, "/accounts")) {
                assertEquals(HttpStatusCode.Created, response.status())
                assertTrue(response.contentType().match(ContentType.Application.Json))
                val account = mapper.readValue<Account>(assertNotNull(response.content))
                assertTrue(account.id > 0)
                assertTrue(account.number.isNotBlank())
                assertEquals(AccountStatuses.OPEN, account.status)
                account
            }
            with(handleRequest(HttpMethod.Post, "/accounts/${accountOne.id}/transfers") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(mapper.writeValueAsString(TransferRequest(accountTwo.id, 100.toBigDecimal())))
            }) {
                assertEquals(HttpStatusCode.Created, response.status())
                assertTrue(response.contentType().match(ContentType.Application.Json))
                val (transactionOne, transactionTwo) = mapper.readValue<TransactionsCreatedResponse>(
                    assertNotNull(
                        response.content
                    )
                )
                assertEquals(100.toBigDecimal(), transactionOne.amount)
                assertEquals(100.toBigDecimal(), transactionTwo.amount)
                assertEquals(accountOne.id, transactionOne.pairAccountId)
                assertEquals(accountTwo.id, transactionTwo.pairAccountId)
                assertEquals(TransactionType.DEPOSIT, transactionOne.type)
                assertEquals(TransactionType.WITHDRAWAL, transactionTwo.type)
            }
        }
    }

    @Test
    fun shouldDeclineTransfer() {
        withTestApplication({ module() }) {
            val accountOne = with(handleRequest(HttpMethod.Post, "/accounts")) {
                assertEquals(HttpStatusCode.Created, response.status())
                assertTrue(response.contentType().match(ContentType.Application.Json))
                val account = mapper.readValue<Account>(assertNotNull(response.content))
                assertTrue(account.id > 0)
                assertTrue(account.number.isNotBlank())
                assertEquals(AccountStatuses.OPEN, account.status)
                account
            }
            val accountTwo = with(handleRequest(HttpMethod.Post, "/accounts")) {
                assertEquals(HttpStatusCode.Created, response.status())
                assertTrue(response.contentType().match(ContentType.Application.Json))
                val account = mapper.readValue<Account>(assertNotNull(response.content))
                assertTrue(account.id > 0)
                assertTrue(account.number.isNotBlank())
                assertEquals(AccountStatuses.OPEN, account.status)
                account
            }
            with(handleRequest(HttpMethod.Post, "/accounts/${accountOne.id}/transfers") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(mapper.writeValueAsString(TransferRequest(accountTwo.id, 100.toBigDecimal())))
            }) {
                assertEquals(HttpStatusCode.Conflict, response.status())
            }
        }
    }

    @Test
    fun shouldDeclineTransferWithClosedAccount() {
        withTestApplication({ module() }) {
            val accountOne = with(handleRequest(HttpMethod.Post, "/accounts")) {
                assertEquals(HttpStatusCode.Created, response.status())
                assertTrue(response.contentType().match(ContentType.Application.Json))
                val account = mapper.readValue<Account>(assertNotNull(response.content))
                assertTrue(account.id > 0)
                assertTrue(account.number.isNotBlank())
                assertEquals(AccountStatuses.OPEN, account.status)
                account
            }
            val accountTwo = with(handleRequest(HttpMethod.Post, "/accounts")) {
                assertEquals(HttpStatusCode.Created, response.status())
                assertTrue(response.contentType().match(ContentType.Application.Json))
                val account = mapper.readValue<Account>(assertNotNull(response.content))
                assertTrue(account.id > 0)
                assertTrue(account.number.isNotBlank())
                assertEquals(AccountStatuses.OPEN, account.status)
                account
            }
            handleRequest(HttpMethod.Delete, "/accounts/${accountOne.id}").apply {
                assertEquals(HttpStatusCode.Accepted, response.status())
                val deleted = mapper.readValue<Account>(assertNotNull(response.content))
                assertEquals(AccountStatuses.CLOSED, deleted.status)
                assertEquals(accountOne.id, deleted.id)
                assertEquals(accountOne.number, deleted.number)
            }
            with(handleRequest(HttpMethod.Post, "/accounts/${accountOne.id}/transfers") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(mapper.writeValueAsString(TransferRequest(accountTwo.id, 100.toBigDecimal())))
            }) {
                assertEquals(HttpStatusCode.Conflict, response.status())
            }
        }
    }
}

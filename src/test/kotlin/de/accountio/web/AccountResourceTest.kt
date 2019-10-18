package de.accountio.web

import de.accountio.callDelete
import de.accountio.callGet
import de.accountio.callPost
import de.accountio.domain.Account
import de.accountio.domain.AccountStatuses
import de.accountio.domain.TransactionType
import de.accountio.responseBody
import de.accountio.testApp
import de.accountio.with
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.contentType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AccountResourceTest {
    @Test
    fun shouldCreateNewAccount() = testApp {
        val createdAccount = callPost(uri = "/accounts").with {
            assertEquals(HttpStatusCode.Created, status())
            assertTrue(contentType().match(ContentType.Application.Json))
            val account = responseBody<Account>()
            assertTrue(account.id > 0)
            assertTrue(account.number.isNotBlank())
            assertEquals(account.status, AccountStatuses.OPEN)
            account
        }
        callGet("/accounts/${createdAccount.id}").with {
            assertEquals(HttpStatusCode.OK, status())
            assertTrue(contentType().match(ContentType.Application.Json))
            val account = responseBody<Account>()
            assertEquals(createdAccount, account)
        }
    }

    @Test
    fun shouldCreateNewAccountWithInitialBalance() = testApp {
        val accountId = callPost("/accounts", CreateAccountRequest(100.toBigDecimal())).with {
            assertEquals(HttpStatusCode.Created, status())
            assertTrue(contentType().match(ContentType.Application.Json))
            val account = responseBody<Account>()
            assertTrue(account.id > 0)
            assertTrue(account.number.isNotBlank())
            assertEquals(account.status, AccountStatuses.OPEN)
            account.id
        }
        callGet("/accounts/$accountId/balance").with {
            assertEquals(HttpStatusCode.OK, status())
            assertTrue(contentType().match(ContentType.Application.Json))
            val balanceResponse = responseBody<AccountBalanceResponse>()
            assertEquals(100.toBigDecimal(), balanceResponse.balance)
        }
    }

    @Test
    fun shouldBadRequestWhenFindingAccountByInvalidId() = testApp {
        callGet("/accounts/invalidId").with {
            assertEquals(HttpStatusCode.BadRequest, status())
            assertTrue(contentType().match(ContentType.Application.Json))
            val response = responseBody<Map<String, String>>()
            assertEquals(response["error"], "Must provide id")
        }
    }

    @Test
    fun shouldNotFoundWhenAccountNotFoundOnFindById() = testApp {
        callGet("/accounts/10").with {
            assertEquals(HttpStatusCode.NotFound, status())
            val response = responseBody<Map<String, Int>>()
            assertEquals(response["id"], 10)
        }
    }

    @Test
    fun shouldNotFoundWhenAccountNotFoundOnDelete() = testApp {
        callDelete("/accounts/10").apply {
            assertEquals(HttpStatusCode.NotFound, status())
            val response = responseBody<Map<String, Int>>()
            assertEquals(response["id"], 10)
        }
    }

    @Test
    fun shouldBadRequestWhenDeleteAccountByInvalidId() = testApp {
        callDelete("/accounts/invalidId").with {
            assertEquals(HttpStatusCode.BadRequest, status())
            assertTrue(contentType().match(ContentType.Application.Json))
            val response = responseBody<Map<String, String>>()
            assertEquals(response["error"], "Must provide id")
        }
    }

    @Test
    fun shouldReturnClosedAccountOnDelete() = testApp {
        callPost("/accounts").with {
            assertEquals(HttpStatusCode.Created, status())
            assertTrue(contentType().match(ContentType.Application.Json))
            val account = responseBody<Account>()
            assertTrue(account.id > 0)
            assertTrue(account.number.isNotBlank())
            assertEquals(AccountStatuses.OPEN, account.status)

            callDelete("/accounts/${account.id}").with {
                assertEquals(HttpStatusCode.Accepted, status())
                val deleted = responseBody<Account>()
                assertEquals(AccountStatuses.CLOSED, deleted.status)
                assertEquals(account.id, deleted.id)
                assertEquals(account.number, deleted.number)
            }
        }
    }

    @Test
    fun shouldCreateTransfer() = testApp {
        val accountOne = callPost("/accounts", CreateAccountRequest(100.toBigDecimal())).with {
            assertEquals(HttpStatusCode.Created, status())
            assertTrue(contentType().match(ContentType.Application.Json))
            val account = responseBody<Account>()
            assertTrue(account.id > 0)
            assertTrue(account.number.isNotBlank())
            assertEquals(AccountStatuses.OPEN, account.status)
            account
        }
        val accountTwo = callPost("/accounts").with {
            assertEquals(HttpStatusCode.Created, status())
            assertTrue(contentType().match(ContentType.Application.Json))
            val account = responseBody<Account>()
            assertTrue(account.id > 0)
            assertTrue(account.number.isNotBlank())
            assertEquals(AccountStatuses.OPEN, account.status)
            account
        }
        callPost("/accounts/${accountOne.id}/transfers", TransferRequest(accountTwo.id, 100.toBigDecimal())).with {
            assertEquals(HttpStatusCode.Created, status())
            assertTrue(contentType().match(ContentType.Application.Json))
            val (transactionOne, transactionTwo) = responseBody<TransactionsCreatedResponse>()
            assertEquals(100.toBigDecimal(), transactionOne.amount)
            assertEquals(100.toBigDecimal(), transactionTwo.amount)
            assertEquals(accountOne.id, transactionOne.pairAccountId)
            assertEquals(accountTwo.id, transactionTwo.pairAccountId)
            assertEquals(TransactionType.DEPOSIT, transactionOne.type)
            assertEquals(TransactionType.WITHDRAWAL, transactionTwo.type)
        }
    }

    @Test
    fun shouldDeclineTransfer() = testApp {
        val accountOne = callPost("/accounts").with {
            assertEquals(HttpStatusCode.Created, status())
            assertTrue(contentType().match(ContentType.Application.Json))
            val account = responseBody<Account>()
            assertTrue(account.id > 0)
            assertTrue(account.number.isNotBlank())
            assertEquals(AccountStatuses.OPEN, account.status)
            account
        }
        val accountTwo = callPost("/accounts").with {
            assertEquals(HttpStatusCode.Created, status())
            assertTrue(contentType().match(ContentType.Application.Json))
            val account = responseBody<Account>()
            assertTrue(account.id > 0)
            assertTrue(account.number.isNotBlank())
            assertEquals(AccountStatuses.OPEN, account.status)
            account
        }
        callPost("/accounts/${accountOne.id}/transfers", TransferRequest(accountTwo.id, 100.toBigDecimal())).with {
            assertEquals(HttpStatusCode.Conflict, status())
        }
    }

    @Test
    fun shouldDeclineTransferWithClosedAccount() = testApp {
        val accountOne = callPost("/accounts").with {
            assertEquals(HttpStatusCode.Created, status())
            assertTrue(contentType().match(ContentType.Application.Json))
            val account = responseBody<Account>()
            assertTrue(account.id > 0)
            assertTrue(account.number.isNotBlank())
            assertEquals(AccountStatuses.OPEN, account.status)
            account
        }
        val accountTwo = callPost("/accounts").with {
            assertEquals(HttpStatusCode.Created, status())
            assertTrue(contentType().match(ContentType.Application.Json))
            val account = responseBody<Account>()
            assertTrue(account.id > 0)
            assertTrue(account.number.isNotBlank())
            assertEquals(AccountStatuses.OPEN, account.status)
            account
        }
        callDelete("/accounts/${accountOne.id}").with {
            assertEquals(HttpStatusCode.Accepted, status())
            val deleted = responseBody<Account>()
            assertEquals(AccountStatuses.CLOSED, deleted.status)
            assertEquals(accountOne.id, deleted.id)
            assertEquals(accountOne.number, deleted.number)
        }
        callPost("/accounts/${accountOne.id}/transfers", TransferRequest(accountTwo.id, 100.toBigDecimal())).with {
            assertEquals(HttpStatusCode.Conflict, status())
        }
    }
}

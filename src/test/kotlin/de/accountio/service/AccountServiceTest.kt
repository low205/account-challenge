package de.accountio.service

import de.accountio.await
import de.accountio.domain.Account
import de.accountio.domain.AccountStatuses
import de.accountio.domain.Transaction
import de.accountio.domain.TransactionType
import de.accountio.store.AccountStorage
import de.accountio.store.EntityNotFoundException
import de.accountio.use
import de.accountio.web.TransferRequest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class AccountServiceTest {

    @Test
    fun shouldCreateNewAccount() = runBlockingTest {
        accountServiceActor(AccountStorage()).use { accountService ->
            val account = await<Account> {
                accountService.send(CreateNewAccount(result = it))
            }
            assertTrue(account.id > 0)
            assertTrue(account.number.isNotBlank())
            assertEquals(account.status, AccountStatuses.OPEN)
        }
    }

    @Test
    fun shouldFailToFindAccountById() = runBlockingTest {
        accountServiceActor(AccountStorage()).use { accountService ->
            val exception = assertFails {
                await<Account> {
                    accountService.send(FindAccountById(-10, it))
                }
            }
            assertTrue(exception is EntityNotFoundException)
        }
    }

    @Test
    fun shouldFindAccountById() = runBlockingTest {
        accountServiceActor(AccountStorage()).use { accountService ->
            val account = await<Account> {
                accountService.send(CreateNewAccount(result = it))
            }
            val accountFound = await<Account> {
                accountService.send(FindAccountById(account.id, it))
            }
            assertEquals(account, accountFound)
        }
    }

    @Test
    fun shouldCloseAccount() = runBlockingTest {
        accountServiceActor(AccountStorage()).use { accountService ->
            val account = await<Account> {
                accountService.send(CreateNewAccount(result = it))
            }
            val accountClosed = await<Account> {
                accountService.send(CloseAccount(result = it, id = account.id))
            }
            assertEquals(account.id, account.id)
            assertEquals(account.number, account.number)
            assertEquals(AccountStatuses.CLOSED, accountClosed.status)
        }
    }

    @Test
    fun shouldCreateAccountWithNonZeroBalance() = runBlockingTest {
        accountServiceActor(AccountStorage()).use { accountService ->
            val account = await<Account> {
                accountService.send(CreateNewAccount(initialAmount = 100.toBigDecimal(), result = it))
            }
            assertTrue(account.id > 0)
            assertTrue(account.number.isNotBlank())
            assertEquals(account.status, AccountStatuses.OPEN)
            val balance = await<BigDecimal> {
                accountService.send(GetAccountBalance(account.id, it))
            }
            assertEquals(100.toBigDecimal(), balance)
        }
    }

    @Test
    fun shouldThrowWhenAskingBalanceOfNonExistentAccount() = runBlockingTest {
        accountServiceActor(AccountStorage()).use { accountService ->
            val exception = assertFails {
                await<BigDecimal> {
                    accountService.send(GetAccountBalance(-10, it))
                }
            }
            assertTrue(exception is EntityNotFoundException)
        }
    }

    @Test
    fun shouldThrowClosingAccountWithNonZeroBalance() = runBlockingTest {
        accountServiceActor(AccountStorage()).use { accountService ->
            val account = await<Account> {
                accountService.send(CreateNewAccount(initialAmount = 100.toBigDecimal(), result = it))
            }
            assertTrue(account.id > 0)
            assertTrue(account.number.isNotBlank())
            assertEquals(account.status, AccountStatuses.OPEN)
            val balance = await<BigDecimal> {
                accountService.send(GetAccountBalance(account.id, it))
            }
            assertEquals(100.toBigDecimal(), balance)
            assertFails {
                await<Account> {
                    accountService.send(CloseAccount(account.id, it))
                }
            }
        }
    }

    @Test
    fun shouldCreateTransfer() = runBlockingTest {
        accountServiceActor(AccountStorage()).use { accountService ->
            val accountOne = await<Account> {
                accountService.send(CreateNewAccount(initialAmount = 100.toBigDecimal(), result = it))
            }
            val accountTwo = await<Account> {
                accountService.send(CreateNewAccount(result = it))
            }
            val request = TransferRequest(
                accountTwo.id, 50.toBigDecimal()
            )
            val (transactionOne, transactionTwo) = await<Pair<Transaction, Transaction>> {
                accountService.send(TransferAmount(accountOne.id, request, it))
            }
            assertEquals(50.toBigDecimal(), transactionOne.amount)
            assertEquals(50.toBigDecimal(), transactionTwo.amount)
            assertEquals(accountOne.id, transactionOne.pairAccountId)
            assertEquals(accountTwo.id, transactionTwo.pairAccountId)
            assertEquals(TransactionType.DEPOSIT, transactionOne.type)
            assertEquals(TransactionType.WITHDRAWAL, transactionTwo.type)
        }
    }

    @Test
    fun shouldThrowOnTransferFromClosedAccount() = runBlockingTest {
        accountServiceActor(AccountStorage()).use { accountService ->
            val accountOne = await<Account> {
                accountService.send(CreateNewAccount(result = it))
            }
            val accountTwo = await<Account> {
                accountService.send(CreateNewAccount(result = it))
            }
            await<Account> {
                accountService.send(CloseAccount(accountOne.id, result = it))
            }
            val request = TransferRequest(
                accountTwo.id, 50.toBigDecimal()
            )
            val exception = assertFails {
                await<Pair<Transaction, Transaction>> {
                    accountService.send(TransferAmount(accountOne.id, request, it))
                }
            }
            assertTrue(exception is InvalidAccountStatus)
            assertEquals("Account is in invalid status: CLOSED", exception.message)
        }
    }

    @Test
    fun shouldThrowOnTransferToClosedAccount() = runBlockingTest {
        accountServiceActor(AccountStorage()).use { accountService ->
            val accountOne = await<Account> {
                accountService.send(CreateNewAccount(initialAmount = 100.toBigDecimal(), result = it))
            }
            val accountTwo = await<Account> {
                accountService.send(CreateNewAccount(result = it))
            }
            await<Account> {
                accountService.send(CloseAccount(accountTwo.id, result = it))
            }
            val request = TransferRequest(
                accountTwo.id, 50.toBigDecimal()
            )
            val exception = assertFails {
                await<Pair<Transaction, Transaction>> {
                    accountService.send(TransferAmount(accountOne.id, request, it))
                }
            }
            assertTrue(exception is InvalidAccountStatus)
            assertEquals("Account is in invalid status: CLOSED", exception.message)
        }
    }

    @Test
    fun shouldThrowOnTransferFromAccountWithLessBalance() = runBlockingTest {
        accountServiceActor(AccountStorage()).use { accountService ->
            val accountOne = await<Account> {
                accountService.send(CreateNewAccount(initialAmount = 100.toBigDecimal(), result = it))
            }
            val accountTwo = await<Account> {
                accountService.send(CreateNewAccount(result = it))
            }
            val request = TransferRequest(
                accountTwo.id, 150.toBigDecimal()
            )
            val exception = assertFails {
                await<Pair<Transaction, Transaction>> {
                    accountService.send(TransferAmount(accountOne.id, request, it))
                }
            }
            assertTrue(exception is InvalidAccountBalance)
            assertEquals("Balance is expected to be 150. But available balance is 100", exception.message)
        }
    }

    @Test
    fun shouldThrowOnTransferToNotFoundAccount() = runBlockingTest {
        accountServiceActor(AccountStorage()).use { accountService ->
            val accountOne = await<Account> {
                accountService.send(CreateNewAccount(initialAmount = 100.toBigDecimal(), result = it))
            }
            val request = TransferRequest(
                -10, 50.toBigDecimal()
            )
            val exception = assertFails {
                await<Pair<Transaction, Transaction>> {
                    accountService.send(TransferAmount(accountOne.id, request, it))
                }
            }
            assertTrue(exception is EntityNotFoundException)
        }
    }

    @Test
    fun shouldThrowOnTransferWithFromNotFoundAccount() = runBlockingTest {
        accountServiceActor(AccountStorage()).use { accountService ->
            val accountOne = await<Account> {
                accountService.send(CreateNewAccount(initialAmount = 100.toBigDecimal(), result = it))
            }
            val request = TransferRequest(
                accountOne.id, 50.toBigDecimal()
            )
            val exception = assertFails {
                await<Pair<Transaction, Transaction>> {
                    accountService.send(TransferAmount(-10, request, it))
                }
            }
            assertTrue(exception is EntityNotFoundException)
        }
    }

    @Test
    fun shouldFindNoAccounts() = runBlockingTest {
        accountServiceActor(AccountStorage()).use { accountService ->
            val findResponse = await<PageResponse<Account>> {
                accountService.send(FindAccounts(PageRequest(10, ""), it))
            }
            assertTrue(findResponse.accounts.isEmpty())
        }
    }

    @Test
    fun shouldFindAccounts() = runBlockingTest {
        accountServiceActor(AccountStorage()).use { accountService ->
            await<Account> {
                accountService.send(CreateNewAccount(initialAmount = 100.toBigDecimal(), result = it))
            }
            await<Account> {
                accountService.send(CreateNewAccount(initialAmount = 100.toBigDecimal(), result = it))
            }
            await<Account> {
                accountService.send(CreateNewAccount(initialAmount = 100.toBigDecimal(), result = it))
            }
            val findResponse = await<PageResponse<Account>> {
                accountService.send(FindAccounts(PageRequest(10, ""), it))
            }
            assertEquals(3, findResponse.accounts.size)
        }
    }

    @Test
    fun shouldListAccountsWithLimit() = runBlockingTest {
        accountServiceActor(AccountStorage()).use { accountService ->
            await<Account> {
                accountService.send(CreateNewAccount(initialAmount = 100.toBigDecimal(), result = it))
            }
            await<Account> {
                accountService.send(CreateNewAccount(initialAmount = 100.toBigDecimal(), result = it))
            }
            await<Account> {
                accountService.send(CreateNewAccount(initialAmount = 100.toBigDecimal(), result = it))
            }
            val findResponse = await<PageResponse<Account>> {
                accountService.send(FindAccounts(PageRequest(2, ""), it))
            }
            assertEquals(2, findResponse.accounts.size)
            accountService.close()
        }
    }

    @Test
    fun shouldCursorThroughAccounts() = runBlockingTest {
        accountServiceActor(AccountStorage()).use { accountService ->
            val account1 = await<Account> {
                accountService.send(CreateNewAccount(initialAmount = 100.toBigDecimal(), result = it))
            }
            val account2 = await<Account> {
                accountService.send(CreateNewAccount(initialAmount = 100.toBigDecimal(), result = it))
            }
            val account3 = await<Account> {
                accountService.send(CreateNewAccount(initialAmount = 100.toBigDecimal(), result = it))
            }
            val findResponse1 = await<PageResponse<Account>> {
                accountService.send(FindAccounts(PageRequest(2, ""), it))
            }
            assertEquals(2, findResponse1.accounts.size)
            assertEquals(listOf(account1, account2), findResponse1.accounts)
            val findResponse2 = await<PageResponse<Account>> {
                accountService.send(FindAccounts(PageRequest(2, findResponse1.nextCursor), it))
            }
            assertEquals(1, findResponse2.accounts.size)
            assertEquals(listOf(account3), findResponse2.accounts)
        }
    }
}

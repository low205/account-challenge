package de.accountio.service

import de.accountio.await
import de.accountio.domain.Account
import de.accountio.domain.AccountStatuses
import de.accountio.domain.Transaction
import de.accountio.domain.TransactionType
import de.accountio.store.EntityNotFoundException
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
        val accountService = accountServiceActor()
        val account = await<Account> {
            accountService.send(AccountServiceCommand.CreateNewAccount(result = it))
        }
        assertTrue(account.id > 0)
        assertTrue(account.number.isNotBlank())
        assertEquals(account.status, AccountStatuses.OPEN)
        accountService.close()
    }

    @Test
    fun shouldFailToFindAccountById() {
        runBlockingTest {
            val accountService = accountServiceActor()
            val exception = assertFails {
                await<Account> {
                    accountService.send(AccountServiceCommand.FindAccountById(-10, it))
                }
            }
            assertTrue(exception is EntityNotFoundException)
            accountService.close()
        }
    }

    @Test
    fun shouldFindAccountById() = runBlockingTest {
        val accountService = accountServiceActor()
        val account = await<Account> {
            accountService.send(AccountServiceCommand.CreateNewAccount(result = it))
        }
        val accountFound = await<Account> {
            accountService.send(AccountServiceCommand.FindAccountById(account.id, it))
        }
        assertEquals(account, accountFound)
        accountService.close()
    }

    @Test
    fun shouldCloseAccount() = runBlockingTest {
        val accountService = accountServiceActor()
        val account = await<Account> {
            accountService.send(AccountServiceCommand.CreateNewAccount(result = it))
        }
        val accountClosed = await<Account> {
            accountService.send(AccountServiceCommand.CloseAccount(result = it, id = account.id))
        }
        assertEquals(account.id, account.id)
        assertEquals(account.number, account.number)
        assertEquals(AccountStatuses.CLOSED, accountClosed.status)
        accountService.close()
    }

    @Test
    fun shouldCreateAccountWithNonZeroBalance() = runBlockingTest {
        val accountService = accountServiceActor()
        val account = await<Account> {
            accountService.send(AccountServiceCommand.CreateNewAccount(initialAmount = 100.toBigDecimal(), result = it))
        }
        assertTrue(account.id > 0)
        assertTrue(account.number.isNotBlank())
        assertEquals(account.status, AccountStatuses.OPEN)
        val balance = await<BigDecimal> {
            accountService.send(AccountServiceCommand.GetAccountBalance(account.id, it))
        }
        assertEquals(100.toBigDecimal(), balance)
        accountService.close()
    }

    @Test
    fun shouldThrowWhenAskingBalanceOfNonExistentAccount() = runBlockingTest {
        val accountService = accountServiceActor()
        val exception = assertFails {
            await<BigDecimal> {
                accountService.send(AccountServiceCommand.GetAccountBalance(-10, it))
            }
        }
        assertTrue(exception is EntityNotFoundException)
        accountService.close()
    }

    @Test
    fun shouldThrowClosingAccountWithNonZeroBalance() = runBlockingTest {
        val accountService = accountServiceActor()
        val account = await<Account> {
            accountService.send(AccountServiceCommand.CreateNewAccount(initialAmount = 100.toBigDecimal(), result = it))
        }
        assertTrue(account.id > 0)
        assertTrue(account.number.isNotBlank())
        assertEquals(account.status, AccountStatuses.OPEN)
        val balance = await<BigDecimal> {
            accountService.send(AccountServiceCommand.GetAccountBalance(account.id, it))
        }
        assertEquals(100.toBigDecimal(), balance)
        assertFails {
            await<Account> {
                accountService.send(AccountServiceCommand.CloseAccount(account.id, it))
            }
        }
        accountService.close()
    }

    @Test
    fun shouldCreateTransfer() = runBlockingTest {
        val accountService = accountServiceActor()
        val accountOne = await<Account> {
            accountService.send(AccountServiceCommand.CreateNewAccount(initialAmount = 100.toBigDecimal(), result = it))
        }
        val accountTwo = await<Account> {
            accountService.send(AccountServiceCommand.CreateNewAccount(result = it))
        }
        val request = TransferRequest(
            accountTwo.id, 50.toBigDecimal()
        )
        val (transactionOne, transactionTwo) = await<Pair<Transaction, Transaction>> {
            accountService.send(AccountServiceCommand.TransferAmount(accountOne.id, request, it))
        }
        assertEquals(50.toBigDecimal(), transactionOne.amount)
        assertEquals(50.toBigDecimal(), transactionTwo.amount)
        assertEquals(accountOne.id, transactionOne.pairAccountId)
        assertEquals(accountTwo.id, transactionTwo.pairAccountId)
        assertEquals(TransactionType.DEPOSIT, transactionOne.type)
        assertEquals(TransactionType.WITHDRAWAL, transactionTwo.type)
        accountService.close()
    }

    @Test
    fun shouldThrowOnTransferFromClosedAccount() = runBlockingTest {
        val accountService = accountServiceActor()
        val accountOne = await<Account> {
            accountService.send(AccountServiceCommand.CreateNewAccount(result = it))
        }
        val accountTwo = await<Account> {
            accountService.send(AccountServiceCommand.CreateNewAccount(result = it))
        }
        await<Account> {
            accountService.send(AccountServiceCommand.CloseAccount(accountOne.id, result = it))
        }
        val request = TransferRequest(
            accountTwo.id, 50.toBigDecimal()
        )
        assertFails {
            await<Pair<Transaction, Transaction>> {
                accountService.send(AccountServiceCommand.TransferAmount(accountOne.id, request, it))
            }
        }
        accountService.close()
    }

    @Test
    fun shouldThrowOnTransferToClosedAccount() = runBlockingTest {
        val accountService = accountServiceActor()
        val accountOne = await<Account> {
            accountService.send(AccountServiceCommand.CreateNewAccount(initialAmount = 100.toBigDecimal(), result = it))
        }
        val accountTwo = await<Account> {
            accountService.send(AccountServiceCommand.CreateNewAccount(result = it))
        }
        await<Account> {
            accountService.send(AccountServiceCommand.CloseAccount(accountTwo.id, result = it))
        }
        val request = TransferRequest(
            accountTwo.id, 50.toBigDecimal()
        )
        assertFails {
            await<Pair<Transaction, Transaction>> {
                accountService.send(AccountServiceCommand.TransferAmount(accountOne.id, request, it))
            }
        }
        accountService.close()
    }

    @Test
    fun shouldThrowOnTransferFromAccountWithLessBalance() = runBlockingTest {
        val accountService = accountServiceActor()
        val accountOne = await<Account> {
            accountService.send(AccountServiceCommand.CreateNewAccount(initialAmount = 100.toBigDecimal(), result = it))
        }
        val accountTwo = await<Account> {
            accountService.send(AccountServiceCommand.CreateNewAccount(result = it))
        }
        val request = TransferRequest(
            accountTwo.id, 150.toBigDecimal()
        )
        assertFails {
            await<Pair<Transaction, Transaction>> {
                accountService.send(AccountServiceCommand.TransferAmount(accountOne.id, request, it))
            }
        }
        accountService.close()
    }

    @Test
    fun shouldThrowOnTransferToNotFoundAccount() = runBlockingTest {
        val accountService = accountServiceActor()
        val accountOne = await<Account> {
            accountService.send(AccountServiceCommand.CreateNewAccount(initialAmount = 100.toBigDecimal(), result = it))
        }
        val request = TransferRequest(
            -10, 50.toBigDecimal()
        )
        val exception = assertFails {
            await<Pair<Transaction, Transaction>> {
                accountService.send(AccountServiceCommand.TransferAmount(accountOne.id, request, it))
            }
        }
        assertTrue(exception is EntityNotFoundException)
        accountService.close()
    }

    @Test
    fun shouldThrowOnTransferWithFromNotFoundAccount() = runBlockingTest {
        val accountService = accountServiceActor()
        val accountOne = await<Account> {
            accountService.send(AccountServiceCommand.CreateNewAccount(initialAmount = 100.toBigDecimal(), result = it))
        }
        val request = TransferRequest(
            accountOne.id, 50.toBigDecimal()
        )
        val exception = assertFails {
            await<Pair<Transaction, Transaction>> {
                accountService.send(AccountServiceCommand.TransferAmount(-10, request, it))
            }
        }
        assertTrue(exception is EntityNotFoundException)
        accountService.close()
    }
}

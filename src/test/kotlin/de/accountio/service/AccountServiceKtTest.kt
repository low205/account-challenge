package de.accountio.service

import de.accountio.await
import de.accountio.domain.Account
import de.accountio.domain.AccountStatuses
import de.accountio.store.EntityNotFoundException
import kotlinx.coroutines.test.runBlockingTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertTrue

internal class AccountServiceTest {

    @Test
    fun shouldCreateNewAccount() = runBlockingTest {
        val accountService = accountServiceActor()
        val account = await<Account> {
            accountService.send(AccountServiceCommand.CreateNewAccount(it))
        }
        assertTrue(account.id > 0)
        assertTrue(account.number.isNotBlank())
        assertEquals(account.status, AccountStatuses.OPEN)
        accountService.close()
    }

    @Test
    fun shouldFailToFindAccountById() = runBlockingTest {
        val accountService = accountServiceActor()
        val exception = assertFails {
            await<Account> {
                accountService.send(AccountServiceCommand.FindAccountById(-10, it))
            }
        }
        assertTrue(exception is EntityNotFoundException)
        accountService.close()
    }

    @Test
    fun shouldFindAccountById() = runBlockingTest {
        val accountService = accountServiceActor()
        val account = await<Account> {
            accountService.send(AccountServiceCommand.CreateNewAccount(it))
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
            accountService.send(AccountServiceCommand.CreateNewAccount(it))
        }
        val accountClosed = await<Account> {
            accountService.send(AccountServiceCommand.CloseAccount(account.id, it))
        }
        assertEquals(account.id, accountClosed.id)
        assertEquals(account.number, accountClosed.number)
        assertEquals(AccountStatuses.CLOSED, accountClosed.status)
        accountService.close()
    }
}

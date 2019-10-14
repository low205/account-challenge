package de.accountio.service

import de.accountio.de.accountio.domain.Account
import de.accountio.de.accountio.domain.AccountStatuses
import de.accountio.de.accountio.service.AccountServiceCommand
import de.accountio.de.accountio.service.accountServiceActor
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runBlockingTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class AccountServiceTest {

    @Test
    fun shouldCreateNewAccount() = runBlockingTest {
        val accountService = accountServiceActor()
        val accountDeferred = CompletableDeferred<Account>()
        accountService.send(AccountServiceCommand.CreateNewAccount(accountDeferred))
        val account = accountDeferred.await()
        assertTrue(account.id > 0)
        assertTrue(account.number.isNotBlank())
        assertEquals(account.status, AccountStatuses.OPEN)
        accountService.close()
    }
}

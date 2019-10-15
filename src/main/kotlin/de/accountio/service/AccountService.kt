package de.accountio.service

import de.accountio.domain.Account
import de.accountio.domain.AccountStatuses
import de.accountio.service.AccountServiceCommand.*
import de.accountio.store.EntityNotFoundException
import de.accountio.store.Store
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor

sealed class AccountServiceCommand {
    class CreateNewAccount(val result: CompletableDeferred<Account>) : AccountServiceCommand()
    class FindAccountById(val id: Long, val result: CompletableDeferred<Account>) : AccountServiceCommand()
    class CloseAccount(val id: Long, val result: CompletableDeferred<Account>) : AccountServiceCommand()
}

private object AccountStorage : Store<Account>() {
    fun createNewAccount(): Account {
        val id = nextId()
        val newAccount = Account(id, "businessnumber$id")
        save(newAccount)
        return newAccount
    }
}

fun CoroutineScope.accountServiceActor(): SendChannel<AccountServiceCommand> = actor {
    for (command in channel) {
        when (command) {
            is CreateNewAccount -> command()
            is FindAccountById -> command()
            is CloseAccount -> command()
        }
    }
}

private operator fun CreateNewAccount.invoke() = result.complete(AccountStorage.createNewAccount())

private operator fun FindAccountById.invoke() = AccountStorage.findById(id).let { account ->
    when (account) {
        null -> result.completeExceptionally(EntityNotFoundException(id))
        else -> result.complete(account)
    }
}

private operator fun CloseAccount.invoke() = AccountStorage.findById(id).let { account ->
    when (account) {
        null -> result.completeExceptionally(EntityNotFoundException(id))
        else -> result.complete(AccountStorage.save(account.copy(status = AccountStatuses.CLOSED)))
    }
}

package de.accountio.de.accountio.service

import de.accountio.de.accountio.domain.Account
import de.accountio.de.accountio.store.Store
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor

sealed class AccountServiceCommand {
    class CreateNewAccount(val account: CompletableDeferred<Account>) : AccountServiceCommand()
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
            is AccountServiceCommand.CreateNewAccount -> command.account.complete(AccountStorage.createNewAccount())
        }
    }
}

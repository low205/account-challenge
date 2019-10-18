package de.accountio.service

import de.accountio.domain.Account
import de.accountio.domain.AccountStatuses
import de.accountio.domain.Transaction
import de.accountio.pagination.Paginator
import de.accountio.store.AccountStorage
import de.accountio.use
import de.accountio.web.TransferRequest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import java.math.BigDecimal
import java.math.BigDecimal.ZERO

sealed class AccountServiceCommand {
    abstract operator fun invoke(store: AccountStorage)
}

fun CoroutineScope.accountServiceActor(store: AccountStorage): SendChannel<AccountServiceCommand> = actor {
    for (command in channel) {
        command(store)
    }
}

class CreateNewAccount(private val initialAmount: BigDecimal = ZERO, private val result: CompletableDeferred<Account>) :
    AccountServiceCommand() {
    override fun invoke(store: AccountStorage) = result.use {
        store.createNewAccount(initialAmount)
    }
}

class FindAccountById(private val id: Long, private val result: CompletableDeferred<Account>) :
    AccountServiceCommand() {
    override fun invoke(store: AccountStorage) = result.use {
        store.getById(id)
    }
}

class CloseAccount(private val id: Long, private val result: CompletableDeferred<Account>) : AccountServiceCommand() {
    override fun invoke(store: AccountStorage) = result.use {
        val account = store.getById(id)
        when {
            account.status == AccountStatuses.CLOSED -> account
            else -> {
                val balance = store.balance(account)
                when {
                    balance.signum() == 0 -> store.save(account.copy(status = AccountStatuses.CLOSED))
                    else -> throw InvalidAccountBalance(balance, ZERO)
                }
            }
        }
    }
}

class TransferAmount(
    private val sourceAccount: Long,
    private val transfer: TransferRequest,
    private val result: CompletableDeferred<Pair<Transaction, Transaction>>
) : AccountServiceCommand() {
    override fun invoke(store: AccountStorage) = result.use {
        val source = store.getById(sourceAccount)
        val target = store.getById(transfer.targetAccount)
        val balance = store.balance(source)
        when {
            !source.validForTransfer() -> throw InvalidAccountStatus(source.status)
            !target.validForTransfer() -> throw InvalidAccountStatus(target.status)
            balance >= transfer.amount -> store.transfer(source, target, transfer.amount)
            else -> throw InvalidAccountBalance(balance, transfer.amount)
        }
    }
}

class GetAccountBalance(private val id: Long, private val result: CompletableDeferred<BigDecimal>) :
    AccountServiceCommand() {
    override fun invoke(store: AccountStorage) = result.use {
        val account = store.getById(id)
        store.balance(account)
    }
}

class FindAccounts(
    private val findRequest: PageRequest,
    private val result: CompletableDeferred<PageResponse<Account>>
) :
    AccountServiceCommand() {
    override fun invoke(store: AccountStorage) = result.use {
        val paginator = Paginator(findRequest, store.minIndex)

        val resultList = store
            .getAll()
            .filter { it.id > paginator.edgeId }
            .take(findRequest.limit)

        PageResponse(paginator.nextCursorFor(resultList), resultList)
    }
}

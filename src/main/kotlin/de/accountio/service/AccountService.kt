package de.accountio.service

import de.accountio.domain.Account
import de.accountio.domain.AccountStatuses
import de.accountio.domain.Transaction
import de.accountio.domain.TransactionType
import de.accountio.service.AccountServiceCommand.*
import de.accountio.store.EntityNotFoundException
import de.accountio.store.Store
import de.accountio.sumByBigDecimal
import de.accountio.web.TransferRequest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import java.math.BigDecimal
import java.math.BigDecimal.ZERO
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicLong

sealed class AccountServiceCommand {
    class CreateNewAccount(val initialAmount: BigDecimal = ZERO, val result: CompletableDeferred<Account>) :
        AccountServiceCommand()

    class FindAccountById(val id: Long, val result: CompletableDeferred<Account>) : AccountServiceCommand()
    class CloseAccount(val id: Long, val result: CompletableDeferred<Account>) : AccountServiceCommand()
    class TransferAmount(
        val sourceAccount: Long,
        val transfer: TransferRequest,
        val result: CompletableDeferred<Pair<Transaction, Transaction>>
    ) : AccountServiceCommand()

    class GetAccountBalance(val id: Long, val result: CompletableDeferred<BigDecimal>) : AccountServiceCommand()
}

private object AccountStorage : Store<Account>() {
    private val transactionsSequence: AtomicLong = AtomicLong(0)

    private val payInAccount = Account(nextId(), "initial-pay-in-account").also {
        save(it)
    }

    fun createNewAccount(initialAmount: BigDecimal): Account {
        val id = nextId()
        val newAccount = Account(id, "businessnumber$id")
        save(newAccount)
        if (initialAmount > ZERO) {
            transfer(payInAccount, newAccount, initialAmount)
        }
        return newAccount
    }

    fun transfer(from: Account, to: Account, amount: BigDecimal): Pair<Transaction, Transaction> {
        val currentTime = LocalDateTime.now()
        val depositTransaction = Transaction(
            transactionsSequence.incrementAndGet(), currentTime, TransactionType.DEPOSIT, from.id, amount
        )
        val withdrawTransaction = Transaction(
            transactionsSequence.incrementAndGet(), currentTime, TransactionType.WITHDRAWAL, to.id, amount
        )
        save(to.copy(transactions = to.transactions + depositTransaction))
        save(from.copy(transactions = from.transactions + withdrawTransaction))
        return depositTransaction to withdrawTransaction
    }

    fun balance(source: Account): BigDecimal {
        return source.transactions.map { it.type.amount(it.amount) }.sumByBigDecimal { it }
    }
}

fun CoroutineScope.accountServiceActor(): SendChannel<AccountServiceCommand> = actor {
    for (command in channel) {
        when (command) {
            is CreateNewAccount -> command()
            is FindAccountById -> command()
            is CloseAccount -> command()
            is TransferAmount -> command()
            is GetAccountBalance -> command()
        }
    }
}

private operator fun CreateNewAccount.invoke() = result.complete(AccountStorage.createNewAccount(initialAmount))

private operator fun FindAccountById.invoke() = AccountStorage.findById(id).let { account ->
    when (account) {
        null -> result.completeExceptionally(EntityNotFoundException(id))
        else -> result.complete(account)
    }
}

private operator fun CloseAccount.invoke() = AccountStorage.findById(id).let { account ->
    when {
        account == null -> result.completeExceptionally(EntityNotFoundException(id))
        account.status == AccountStatuses.CLOSED -> result.complete(account)
        else -> {
            val balance = AccountStorage.balance(account)
            when {
                balance.signum() == 0 -> result.complete(AccountStorage.save(account.copy(status = AccountStatuses.CLOSED)))
                else -> result.completeExceptionally(InvalidAccountBalance(balance, ZERO))
            }
        }
    }
}

private operator fun GetAccountBalance.invoke() = AccountStorage.findById(id).let { account ->
    when (account) {
        null -> result.completeExceptionally(EntityNotFoundException(id))
        else -> result.complete(AccountStorage.balance(account))
    }
}

private operator fun TransferAmount.invoke() {
    val source = AccountStorage.findById(sourceAccount)
    val target = AccountStorage.findById(transfer.targetAccount)
    when {
        source == null -> {
            result.completeExceptionally(EntityNotFoundException(sourceAccount))
        }
        target == null -> {
            result.completeExceptionally(EntityNotFoundException(transfer.targetAccount))
        }
        else -> {
            val balance = AccountStorage.balance(source)
            when {
                source.status !in Account.VALID_FOR_TRANSFER -> result.completeExceptionally(InvalidAccountStatus(source.status))
                target.status !in Account.VALID_FOR_TRANSFER -> result.completeExceptionally(InvalidAccountStatus(target.status))
                balance >= transfer.amount -> result.complete(AccountStorage.transfer(source, target, transfer.amount))
                else -> result.completeExceptionally(InvalidAccountBalance(balance, transfer.amount))
            }
        }
    }
}

class InvalidAccountStatus(val status: AccountStatuses) : RuntimeException()
class InvalidAccountBalance(val available: BigDecimal, val expected: BigDecimal) : RuntimeException()

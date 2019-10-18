package de.accountio.store

import de.accountio.domain.Account
import de.accountio.domain.Transaction
import de.accountio.domain.TransactionType
import de.accountio.sumByBigDecimal
import kotlinx.atomicfu.AtomicLong
import kotlinx.atomicfu.atomic
import java.math.BigDecimal
import java.math.BigDecimal.ZERO
import java.time.LocalDateTime

class AccountStorage : Store<Account>() {
    private val transactionsSequence: AtomicLong = atomic(0L)

    private val payInAccount = Account(nextId(), "initial-pay-in-account").also {
        save(it)
    }

    override fun getAll(): List<Account> {
        return super.getAll().filter { it.id != payInAccount.id }
    }

    fun createNewAccount(initialAmount: BigDecimal): Account {
        val id = nextId()
        val newAccount = Account(id, "businessnumber$id")
        save(newAccount)
        if (initialAmount > ZERO) {
            transfer(payInAccount, newAccount, initialAmount)
        }
        return getById(id)
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

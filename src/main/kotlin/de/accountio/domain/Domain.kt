package de.accountio.domain

import de.accountio.store.StorableEntity
import java.math.BigDecimal
import java.time.LocalDateTime

data class Account(
    override val id: Long,
    val number: String,
    val status: AccountStatuses = AccountStatuses.OPEN,
    val transactions: List<Transaction> = emptyList()
) : StorableEntity {
    companion object {
        val VALID_FOR_TRANSFER = setOf(AccountStatuses.OPEN)
    }
}

enum class AccountStatuses {
    OPEN,
    CLOSED
}

enum class TransactionType {
    WITHDRAWAL {
        override fun amount(amount: BigDecimal) = -amount
    },
    DEPOSIT {
        override fun amount(amount: BigDecimal) = amount
    };

    abstract fun amount(amount: BigDecimal): BigDecimal
}

class Transaction(
    override val id: Long,
    val date: LocalDateTime,
    val type: TransactionType,
    val pairAccountId: Long,
    val amount: BigDecimal
) : StorableEntity

package de.accountio.de.accountio.domain

import de.accountio.de.accountio.store.StorableEntity
import java.math.BigDecimal
import java.time.LocalDateTime

data class Account(
    override val id: Long,
    val number: String,
    val status: AccountStatuses = AccountStatuses.OPEN
) : StorableEntity

enum class AccountStatuses {
    OPEN,
    CLOSED
}

class Transaction(
    override val id: Long,
    val date: LocalDateTime,
    val debitAccount: Long,
    val creditAccount: Long,
    val amount: BigDecimal
) : StorableEntity
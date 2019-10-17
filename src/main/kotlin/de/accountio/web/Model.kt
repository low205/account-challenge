package de.accountio.web

import de.accountio.domain.Transaction
import java.math.BigDecimal
import java.time.LocalDateTime

data class TransferRequest(
    val targetAccount: Long,
    val amount: BigDecimal
)

data class CreateAccountRequest(
    val initialAmount: BigDecimal
)

data class TransactionsCreatedResponse(
    val one: Transaction,
    val second: Transaction
)

data class AccountBalanceResponse(
    val balance: BigDecimal,
    val onTime: LocalDateTime
)

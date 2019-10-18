package de.accountio.service

import de.accountio.domain.AccountStatuses
import java.math.BigDecimal

class InvalidAccountStatus(status: AccountStatuses) :
    RuntimeException("Account is in invalid status: $status")

class InvalidAccountBalance(available: BigDecimal, expected: BigDecimal) :
    RuntimeException("Balance is expected to be $expected. But available balance is $available")

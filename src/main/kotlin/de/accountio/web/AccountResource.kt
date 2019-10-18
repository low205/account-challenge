package de.accountio.web

import de.accountio.await
import de.accountio.domain.Account
import de.accountio.domain.Transaction
import de.accountio.service.AccountServiceCommand
import de.accountio.service.CloseAccount
import de.accountio.service.CreateNewAccount
import de.accountio.service.FindAccountById
import de.accountio.service.FindAccounts
import de.accountio.service.GetAccountBalance
import de.accountio.service.PageRequest
import de.accountio.service.PageResponse
import de.accountio.service.TransferAmount
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.request.receiveOrNull
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.delete
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import kotlinx.coroutines.channels.SendChannel
import java.math.BigDecimal
import java.math.BigDecimal.ZERO
import java.time.LocalDateTime

fun Routing.accountResource(accountCommands: SendChannel<AccountServiceCommand>) {
    route("/accounts") {
        route("/{id}") {
            fun ApplicationCall.id() =
                parameters["id"]?.toLongOrNull() ?: throw InvalidRequestException("Must provide id")
            get {
                val id = call.id()
                val account = await<Account> {
                    accountCommands.send(FindAccountById(id, it))
                }
                call.respond(HttpStatusCode.OK, account)
            }
            delete {
                val id = call.id()
                val account = await<Account> {
                    accountCommands.send(CloseAccount(id, it))
                }
                call.respond(HttpStatusCode.Accepted, account)
            }
            get("/balance") {
                val id = call.id()
                val balance = await<BigDecimal> {
                    accountCommands.send(GetAccountBalance(id, it))
                }
                call.respond(HttpStatusCode.OK, AccountBalanceResponse(balance, LocalDateTime.now()))
            }
            post("/transfers") {
                val id = call.id()
                val transfer = call.receive<TransferRequest>()
                val (one, second) = await<Pair<Transaction, Transaction>> {
                    accountCommands.send(TransferAmount(id, transfer, it))
                }
                call.respond(HttpStatusCode.Created, TransactionsCreatedResponse(one, second))
            }
        }
        get {
            val query = call.request.queryParameters
            val limit = query["limit"]?.toIntOrNull() ?: 10
            val next = query["next"] ?: ""

            val result = await<PageResponse<Account>> {
                accountCommands.send(FindAccounts(PageRequest(limit, next), it))
            }
            call.respond(HttpStatusCode.OK, result)
        }
        post {
            val request = call.receiveOrNull<CreateAccountRequest>()
            val account = await<Account> {
                accountCommands.send(
                    CreateNewAccount(
                        initialAmount = request?.initialAmount ?: ZERO,
                        result = it
                    )
                )
            }
            call.respond(HttpStatusCode.Created, account)
        }
    }
}

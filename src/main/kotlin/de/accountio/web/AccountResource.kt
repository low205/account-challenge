package de.accountio.web

import de.accountio.await
import de.accountio.domain.Account
import de.accountio.service.AccountServiceCommand
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import kotlinx.coroutines.channels.SendChannel

fun Routing.accountResource(accountCommands: SendChannel<AccountServiceCommand>) {
    route("/accounts") {
        get("/{id}") {
            val id = call.parameters["id"]?.toLongOrNull() ?: throw InvalidRequestException("Must provide id")
            val account = await<Account> {
                accountCommands.send(AccountServiceCommand.FindAccountById(id, it))
            }
            call.respond(HttpStatusCode.OK, account)
        }
        post("/") {
            val account = await<Account> {
                accountCommands.send(AccountServiceCommand.CreateNewAccount(it))
            }
            call.respond(HttpStatusCode.Created, account)
        }
    }
}

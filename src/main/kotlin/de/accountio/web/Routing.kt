package de.accountio.web

import de.accountio.service.accountServiceActor
import de.accountio.store.AccountStorage
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.route
import io.ktor.routing.routing

fun Application.setupRouting(testing: Boolean = false) {
    val store = AccountStorage()
    val accountService = accountServiceActor(store)

    routing {
        accountResource(accountService)
        if (testing) {
            get("/throw") {
                throw Exception("throw random error")
            }
        }
        get("/") {
            call.respond(mapOf("name" to "Accounting application", "version" to "0.0.1"))
        }
        route("/{...}") {
            handle {
                call.respond(
                    HttpStatusCode.NotFound, mapOf("path" to call.request.local.uri)
                )
            }
        }
    }
}

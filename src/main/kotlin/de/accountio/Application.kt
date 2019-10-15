package de.accountio

import com.fasterxml.jackson.databind.SerializationFeature
import de.accountio.service.accountServiceActor
import de.accountio.store.EntityNotFoundException
import de.accountio.web.InvalidRequestException
import de.accountio.web.accountResource
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.features.DefaultHeaders
import io.ktor.features.StatusPages
import io.ktor.http.HttpStatusCode
import io.ktor.jackson.jackson
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.util.error

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    install(DefaultHeaders)
    install(CallLogging)
    install(ContentNegotiation) {
        jackson {
            enable(SerializationFeature.INDENT_OUTPUT)
        }
    }
    install(StatusPages) {
        exception<EntityNotFoundException> { cause ->
            call.respond(HttpStatusCode.NotFound, mapOf("id" to cause.id))
        }
        exception<InvalidRequestException> { cause ->
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to cause.message))
        }
        exception<Throwable> { cause ->
            environment.log.error(cause)
            call.respond(HttpStatusCode.InternalServerError)
        }
    }
    val accountService = accountServiceActor()
    routing {
        accountResource(accountService)
        get("/") {
            call.respond(mapOf("name" to "Accounting application", "version" to "0.0.1"))
        }
        route("/{param...}") {
            handle {
                call.respond(
                    HttpStatusCode.NotFound, mapOf(
                        "path" to call.parameters
                            .getAll("param")
                            ?.joinToString(
                                separator = "/",
                                prefix = "/"
                            )
                    )
                )
            }
        }
    }
}

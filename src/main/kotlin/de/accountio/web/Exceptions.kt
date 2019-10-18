package de.accountio.web

import de.accountio.service.InvalidAccountBalance
import de.accountio.service.InvalidAccountStatus
import de.accountio.store.EntityNotFoundException
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.StatusPages
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.util.error

class InvalidRequestException(override val message: String?) : RuntimeException()

fun Application.setupExceptions() {
    install(StatusPages) {
        exception<EntityNotFoundException> { cause ->
            call.respond(HttpStatusCode.NotFound, mapOf("id" to cause.id))
        }
        exception<InvalidRequestException> { cause ->
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to cause.message))
        }
        exception<InvalidAccountStatus> { cause ->
            call.respond(HttpStatusCode.Conflict, mapOf("error" to cause.message))
        }
        exception<InvalidAccountBalance> { cause ->
            call.respond(HttpStatusCode.Conflict, mapOf("error" to cause.message))
        }
        exception<Throwable> { cause ->
            environment.log.error(cause)
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to cause.message))
        }
    }
}

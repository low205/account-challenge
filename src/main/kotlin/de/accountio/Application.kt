package de.accountio

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import de.accountio.jackson.JsonMapper
import de.accountio.service.InvalidAccountBalance
import de.accountio.service.InvalidAccountStatus
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
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.jackson.JacksonConverter
import io.ktor.jackson.jackson
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.util.error

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
fun Application.module(testing: Boolean = false) {
    install(DefaultHeaders)
    install(CallLogging)
    install(ContentNegotiation) {
        register(ContentType.Application.Json, JacksonConverter(JsonMapper.mapper))
        jackson {
            enable(SerializationFeature.INDENT_OUTPUT)
            registerModule(JavaTimeModule())
        }
    }
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
    val accountService = accountServiceActor()
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

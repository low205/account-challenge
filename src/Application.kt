package de.accountio

import com.fasterxml.jackson.databind.SerializationFeature
import de.accountio.de.accountio.service.accountServiceActor
import de.accountio.de.accountio.web.accountResource
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.features.DefaultHeaders
import io.ktor.jackson.jackson
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.routing

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
    val accountService = accountServiceActor()
    routing {
        accountResource(accountService)
        get("/") {
            call.respond(mapOf("name" to "Accounting application", "version" to "0.0.1"))
        }
    }
}

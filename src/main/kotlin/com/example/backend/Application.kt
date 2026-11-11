package com.example.backend

import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class User(val username: String, val password: String, val role: String)

@Serializable
data class LoginRequest(val username: String, val password: String)

fun loadCredentials(): List<User> {
    val file = File("src/main/resources/credentials.json")
    return if (file.exists()) {
        // En versiones modernas de kotlinx.serialization no necesitas ListSerializer,
        // puedes inferir el tipo directamente usando el genérico <List<User>>.
        Json.decodeFromString<List<User>>(file.readText())
    } else emptyList()
}

fun main() {
    val users = loadCredentials()
    embeddedServer(Netty, port = 8080) {
        install(ContentNegotiation) {
            json()
        }
        routing {
            post("/login") {
                val credentials = call.receive<LoginRequest>()
                val user = users.find { it.username == credentials.username && it.password == credentials.password }

                if (user != null) {
                    call.respond(mapOf("status" to "ok", "role" to user.role, "apiKey" to "my-secret-api-key"))
                } else {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("status" to "error", "message" to "Invalid credentials"))
                }
            }

            // Example protected endpoint
            get("/protected") {
                val apiKey = call.request.headers["X-Api-Key"]
                if (apiKey == "my-secret-api-key") {
                    call.respond(mapOf("data" to "Secure data for authorized clients"))
                } else {
                    call.respond(HttpStatusCode.Forbidden, mapOf("status" to "error", "message" to "Invalid API key"))
                }
            }
        }
    }.start(wait = true)
}
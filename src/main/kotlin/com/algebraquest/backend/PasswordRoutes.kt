package com.algebraquest.backend

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class ChangePasswordRequest(val username: String, val newPassword: String)

@Serializable
data class MessageResponse(val message: String)

/** POST /admin/users/password  (solo ADMINISTRADOR). Guarda la nueva contraseña en Postgres. */
fun Route.passwordRoutes(store: GameStore) {
    post("/admin/users/password") {
        call.requireAuth(store, ROLE_ADMIN) ?: return@post
        val r = call.receive<ChangePasswordRequest>()
        store.changePassword(r.username, r.newPassword)
        call.respond(MessageResponse("Contraseña de ${r.username} actualizada"))
    }
}

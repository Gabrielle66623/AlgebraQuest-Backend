package com.algebraquest.backend

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.sql.Connection

object DatabaseFactory {
    private var dataSource: HikariDataSource? = null

    fun init() {
        // Lee las variables que agregaste en Railway (o las nativas si las referenciaste directo)
        val host = System.getenv("DB_HOST") ?: System.getenv("MYSQLHOST") ?: System.getenv("PGHOST")
        val port = System.getenv("DB_PORT") ?: System.getenv("MYSQLPORT") ?: System.getenv("PGPORT")
        val dbName = System.getenv("DB_NAME") ?: System.getenv("MYSQLDATABASE") ?: System.getenv("PGDATABASE")
        val user = System.getenv("DB_USER") ?: System.getenv("MYSQLUSER") ?: System.getenv("PGUSER")
        val password = System.getenv("DB_PASSWORD") ?: System.getenv("MYSQLPASSWORD") ?: System.getenv("PGPASSWORD")

        if (host.isNullOrBlank() || dbName.isNullOrBlank()) {
            println("⚠️ Variables de BD no detectadas (Modo Local JSON activo).")
            return
        }

        // Detecta si es Postgres (puerto 5432 o variable PGHOST) o MySQL
        val isPostgres = System.getenv("PGHOST") != null || port == "5432"
        val jdbcUrl = if (isPostgres) {
            "jdbc:postgresql://$host:$port/$dbName"
        } else {
            "jdbc:mysql://$host:$port/$dbName?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
        }

        try {
            val config = HikariConfig().apply {
                this.jdbcUrl = jdbcUrl
                this.username = user
                this.password = password
                this.maximumPoolSize = 5
                this.isAutoCommit = true
                this.driverClassName = if (isPostgres) "org.postgresql.Driver" else "com.mysql.cj.jdbc.Driver"
            }
            dataSource = HikariDataSource(config)
            crearTablasIniciales()
            println("✅ Conectado exitosamente a la Base de Datos en Railway ($host:$port/$dbName)")
        } catch (e: Exception) {
            println("❌ Error al conectar con la Base de Datos en Railway: ${e.message}")
        }
    }

    fun getConnection(): Connection? = dataSource?.connection

    private fun crearTablasIniciales() {
        getConnection()?.use { conn ->
            conn.createStatement().use { stmt ->
                // Tabla clave-valor para persistir tus JSON (jugadores, sesiones, ranking) en la BD de Railway
                stmt.executeUpdate(
                    """
                    CREATE TABLE IF NOT EXISTS app_storage (
                        storage_key VARCHAR(100) PRIMARY KEY,
                        json_data TEXT NOT NULL
                    );
                    """.trimIndent()
                )
            }
        }
    }
}
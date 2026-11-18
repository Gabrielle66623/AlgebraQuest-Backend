package com.algebraquest.backend

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.net.URI
import java.sql.Connection

object DatabaseFactory {
    private var dataSource: HikariDataSource? = null
    private var isPostgres = true

    val isConnected: Boolean get() = dataSource != null

    fun init() {
        var host = System.getenv("DB_HOST") ?: System.getenv("MYSQLHOST") ?: System.getenv("PGHOST")
        var port = System.getenv("DB_PORT") ?: System.getenv("MYSQLPORT") ?: System.getenv("PGPORT")
        var dbName = System.getenv("DB_NAME") ?: System.getenv("MYSQLDATABASE") ?: System.getenv("PGDATABASE")
        var user = System.getenv("DB_USER") ?: System.getenv("MYSQLUSER") ?: System.getenv("PGUSER")
        var password = System.getenv("DB_PASSWORD") ?: System.getenv("MYSQLPASSWORD") ?: System.getenv("PGPASSWORD")
        var postgres = System.getenv("PGHOST") != null

        // Alternativa: DATABASE_URL=postgresql://user:pass@host:port/db (Railway la ofrece)
        if (host.isNullOrBlank() || dbName.isNullOrBlank()) {
            val url = System.getenv("DATABASE_URL") ?: System.getenv("DATABASE_PRIVATE_URL")
            if (!url.isNullOrBlank()) {
                runCatching {
                    val u = URI(url)
                    host = u.host
                    port = if (u.port > 0) u.port.toString() else "5432"
                    dbName = u.path.trimStart('/')
                    val ui = u.userInfo.orEmpty().split(":", limit = 2)
                    user = ui.getOrNull(0)
                    password = ui.getOrNull(1)
                    postgres = u.scheme.startsWith("postgres")
                }
            }
        }

        if (host.isNullOrBlank() || dbName.isNullOrBlank()) {
            println("⚠️ Variables de BD no detectadas (Modo Local JSON activo).")
            return
        }

        isPostgres = postgres || port == "5432"
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
            dataSource = null
            println("❌ Error al conectar con la Base de Datos en Railway: ${e.message}")
        }
    }

    fun getConnection(): Connection? = dataSource?.connection

    private fun crearTablasIniciales() {
        getConnection()?.use { conn ->
            conn.createStatement().use { stmt ->
                stmt.executeUpdate(
                    """
                    CREATE TABLE IF NOT EXISTS app_storage (
                        storage_key VARCHAR(100) PRIMARY KEY,
                        json_data TEXT NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }
    }

    /** Lee el JSON guardado bajo [key] o null si no existe / no hay BD. */
    fun load(key: String): String? = runCatching {
        getConnection()?.use { conn ->
            conn.prepareStatement("SELECT json_data FROM app_storage WHERE storage_key = ?").use { ps ->
                ps.setString(1, key)
                ps.executeQuery().use { rs -> if (rs.next()) rs.getString(1) else null }
            }
        }
    }.onFailure { println("❌ Error leyendo '$key' de la BD: ${it.message}") }.getOrNull()

    /** Guarda (upsert) el JSON bajo [key]. Devuelve true si se guardó. */
    fun save(key: String, data: String): Boolean = runCatching {
        val sql = if (isPostgres) {
            "INSERT INTO app_storage (storage_key, json_data) VALUES (?, ?) " +
                "ON CONFLICT (storage_key) DO UPDATE SET json_data = EXCLUDED.json_data"
        } else {
            "INSERT INTO app_storage (storage_key, json_data) VALUES (?, ?) " +
                "ON DUPLICATE KEY UPDATE json_data = VALUES(json_data)"
        }
        getConnection()?.use { conn ->
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, key)
                ps.setString(2, data)
                ps.executeUpdate()
            }
            true
        } ?: false
    }.onFailure { println("❌ Error guardando '$key' en la BD: ${it.message}") }.getOrDefault(false)
}

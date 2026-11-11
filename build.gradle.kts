plugins {
    kotlin("jvm") version "2.1.0" // <-- Usa 2.1.0 o 2.0.21 (2.4.21 no existe en Maven Central)
    kotlin("plugin.serialization") version "2.1.0"
    application
}

application { mainClass.set("com.algebraquest.backend.ApplicationKt") }

dependencies {
    implementation("io.ktor:ktor-server-netty:3.3.0")
    implementation("io.ktor:ktor-server-content-negotiation:3.3.0")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.3.0")
    implementation("io.ktor:ktor-server-cors:3.3.0")
    implementation("io.ktor:ktor-server-status-pages:3.3.0")
    implementation("ch.qos.logback:logback-classic:1.5.18")

    // 👇 CONEXIÓN A BASE DE DATOS EN RAILWAY 👇
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("com.mysql:mysql-connector-j:8.3.0")       // Si creaste MySQL en Railway
    implementation("org.postgresql:postgresql:42.7.3")        // Si creaste PostgreSQL en Railway

    testImplementation("io.ktor:ktor-server-test-host:3.3.0")
    testImplementation("io.ktor:ktor-client-content-negotiation:3.3.0")
    testImplementation(kotlin("test"))
}

tasks.test { useJUnitPlatform() }

dependencies {
    testImplementation(kotlin("test-junit5"))
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.register("stage") {
    dependsOn("installDist")
}
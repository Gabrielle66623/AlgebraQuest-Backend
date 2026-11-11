# Fase 1: Compilar el proyecto con Gradle y JDK 21 (No requiere gradlew en tu repo)
FROM gradle:8.10-jdk21 AS build
WORKDIR /app
COPY . .
RUN gradle installDist --no-daemon -x test

# Fase 2: Imagen ligera para ejecutar el servidor en Railway
        FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/install/* ./
 COPY --from=build /app /app_source

EXPOSE 8080
CMD ["sh", "-c", "./bin/*"]
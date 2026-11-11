FROM gradle:8.10-jdk21-alpine AS build
WORKDIR /app
COPY . .
ENV GRADLE_OPTS="-Xmx512m -Dorg.gradle.daemon=false"
RUN gradle installDist --no-daemon -x test

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/build/install/* ./
EXPOSE 8080
CMD ["sh", "-c", "./bin/*"]

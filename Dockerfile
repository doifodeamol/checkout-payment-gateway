# Build the application with the same Java major version as the challenge.
FROM gradle:8.2.1-jdk17 AS build
WORKDIR /workspace
COPY . .
RUN gradle clean test bootJar --no-daemon

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd --system --uid 10001 appuser
COPY --from=build /workspace/build/libs/*-0.0.1-SNAPSHOT.jar app.jar
USER appuser
EXPOSE 8090
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]

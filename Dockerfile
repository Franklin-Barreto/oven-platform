FROM eclipse-temurin:25-jdk-alpine AS build

WORKDIR /workspace

COPY .mvn .mvn
COPY mvnw pom.xml ./

RUN chmod +x mvnw \
    && ./mvnw -B -ntp -DskipTests dependency:go-offline

COPY src src
COPY config config

RUN ./mvnw -B -ntp -DskipTests package

FROM eclipse-temurin:25-jre-alpine AS runtime

WORKDIR /app

COPY --from=build /workspace/target/*.jar /app/app.jar

USER 10001:10001
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD wget -q --spider http://localhost:8080/actuator/health/readiness || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]

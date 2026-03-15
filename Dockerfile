FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle gradle.properties lint.gradle ./
COPY core core
COPY storage storage
COPY support support
COPY clients clients
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew && ./gradlew :core:core-api:bootJar -x test

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/core/core-api/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

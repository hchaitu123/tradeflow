# ---------- stage 1: build the jar ----------
FROM eclipse-temurin:21-jdk AS build
ARG SERVICE
WORKDIR /app

# poms first: Maven reads every module listed in the root pom
COPY mvnw pom.xml ./
COPY .mvn .mvn
COPY order-service/pom.xml order-service/
COPY risk-service/pom.xml risk-service/
COPY notification-service/pom.xml notification-service/
COPY ${SERVICE}/src ${SERVICE}/src

# cache mount = keep ~/.m2 between builds, so dependencies aren't re-downloaded each time
RUN --mount=type=cache,target=/root/.m2 ./mvnw -q -pl ${SERVICE} -am package -DskipTests

# ---------- stage 2: run it ----------
FROM eclipse-temurin:21-jre
ARG SERVICE
WORKDIR /app
COPY --from=build /app/${SERVICE}/target/${SERVICE}-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
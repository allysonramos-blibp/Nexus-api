# Etapa 1: Construção (Build) da aplicação usando Java 21
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Etapa 2: Execução (Runtime) da aplicação usando Java 21
FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app
VOLUME /tmp
COPY --from=build /app/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]

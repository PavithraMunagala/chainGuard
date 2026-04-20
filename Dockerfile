FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

COPY backend/riskanalyzer ./backend/riskanalyzer
WORKDIR /app/backend/riskanalyzer

RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jdk
WORKDIR /app

COPY --from=build /app/backend/riskanalyzer/target/*.jar app.jar

EXPOSE 8080
CMD ["java", "-jar", "app.jar"]

# ===========================
# Stage 1: Build the application
# ===========================
# Use Maven with JDK 21 (matches your pom.xml)
FROM maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /app

# Copy only the pom.xml first to cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy the source code and build the jar
COPY src ./src
RUN mvn clean package -DskipTests

# ===========================
# Stage 2: Run the application
# ===========================
# Use a lightweight JRE 21 image
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy the built jar from the build stage
# Note: We use a wildcard (*) because the version might change
COPY --from=build /app/target/*.jar app.jar

# Expose port 8080 (matches your application.properties)
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
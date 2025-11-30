# Stage 1: Build the Spring Boot application
FROM eclipse-temurin:25-jdk-jammy as builder

WORKDIR /app

# Copy the Maven wrapper files and pom.xml to leverage Docker cache
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Download dependencies to leverage Docker cache
RUN ./mvnw dependency:go-offline

# Copy the rest of the application code
COPY src src

# Build the Spring Boot application
RUN ./mvnw clean install -DskipTests

# Stage 2: Create the final image with a JRE for reduced size
FROM eclipse-temurin:25-jre-jammy

WORKDIR /app

# Expose the port your Spring Boot application listens on
EXPOSE 4000

# Copy the built JAR file from the builder stage
COPY --from=builder /app/target/*.jar app.jar

# Command to run the Spring Boot application
ENTRYPOINT ["java", "-jar", "app.jar"]
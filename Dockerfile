# Build stage
FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app

# Copy Maven wrapper files and pom.xml
COPY pom.xml mvnw ./
COPY .mvn .mvn

# Fix potential Windows CRLF line endings and grant execute permissions to mvnw
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw

# Copy source code
COPY src ./src

# Build the application JAR
RUN ./mvnw clean package -DskipTests

# Runtime stage
FROM openjdk:17-jdk-slim
WORKDIR /app

# Copy the generated JAR from build stage
COPY --from=build /app/target/Johuri-0.0.1-SNAPSHOT.jar /app/app.jar

# Expose port 8080
EXPOSE 8080

# Launch application
CMD ["java", "-Dserver.port=8080", "-jar", "/app/app.jar"]

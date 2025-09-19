# Build stage using Microsoft OpenJDK 21
FROM mcr.microsoft.com/openjdk/jdk:21-ubuntu AS build
WORKDIR /workspace/app

# Copy gradle wrapper and build files
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .
RUN chmod +x ./gradlew

# Download dependencies (cached layer)
RUN ./gradlew dependencies --no-daemon

# Copy source code after dependencies
COPY src src

# Build application
RUN ./gradlew bootJar -x test --no-daemon

# Runtime stage using Microsoft OpenJDK 21
FROM mcr.microsoft.com/openjdk/jdk:21-ubuntu
WORKDIR /app

# Copy JAR from build stage
COPY --from=build /workspace/app/build/libs/*.jar /app/
RUN mv /app/*.jar /app/app.jar

# Create volume mount point for images
VOLUME ["/app/static"]

EXPOSE 8080

# JVM optimizations for Java 21 with ZGC
ENTRYPOINT ["java", "-XX:+UseZGC", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
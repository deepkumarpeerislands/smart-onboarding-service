# Build stage
FROM --platform=linux/amd64 gradle:jdk21-jammy AS builder
WORKDIR /app

# Set environment variables for GitHub packages
ARG GITHUB_USERNAME
ARG GITHUB_TOKEN
ARG GITHUB_REPO

ENV GITHUB_USERNAME=$GITHUB_USERNAME
ENV GITHUB_TOKEN=$GITHUB_TOKEN
ENV GITHUB_REPO=$GITHUB_REPO

# Copy only the build configuration files first for better caching
COPY build.gradle settings.gradle ./
COPY gradle gradle

# Copy the rest of the source code
COPY . .

# Build the application
RUN gradle build -x test --no-daemon

# Run stage
FROM --platform=linux/amd64 eclipse-temurin:21-jre-jammy
WORKDIR /app

# Create a non-root user
RUN groupadd -r spring && useradd -r -g spring spring

# Create logs directory and set permissions
RUN mkdir -p /app/logs && \
    chown -R spring:spring /app

# Copy the pre-built JAR file
COPY --from=builder /app/build/libs/smart-onboarding-0.0.1-SNAPSHOT.jar app.jar

# Set ownership to non-root user
RUN chown spring:spring /app/app.jar

# Switch to non-root user
USER spring

EXPOSE 8086

# Health check
HEALTHCHECK --interval=30s --timeout=3s \
  CMD curl -f http://localhost:8086/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
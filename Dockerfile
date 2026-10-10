# =========================================================================
# Multi-stage Dockerfile for Java-Based Online Quiz Platform (Web Module)
#
# Stage 1: Build the deployable WAR package using Maven + Java 17
# Stage 2: Serve the WAR via Apache Tomcat 9 (compatible with Servlet 4.0 / Java 17)
# =========================================================================

# ---- Stage 1: Build ----
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

# Copy pom.xml and source code
COPY pom.xml .
COPY src ./src

# Build production WAR (skip tests here; tests are run during CI / verification)
RUN mvn clean package -DskipTests

# ---- Stage 2: Runtime ----
FROM tomcat:9.0-jre17-temurin-alpine

WORKDIR /usr/local/tomcat

# Remove default sample webapps to keep image clean and secure
RUN rm -rf webapps/*

# Deploy the compiled WAR as ROOT.war so the portal is accessible at root URL "/"
COPY --from=builder /app/target/java-quiz-platform.war webapps/ROOT.war

# Expose standard web port
EXPOSE 8080

# Dynamically bind to $PORT (required by cloud hosts like Railway/Render),
# defaulting to 8080 if PORT is not set.
CMD ["sh", "-c", "sed -i \"s/port=\\\"8080\\\"/port=\\\"${PORT:-8080}\\\"/g\" conf/server.xml && catalina.sh run"]

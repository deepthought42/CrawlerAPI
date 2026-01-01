# Use an official Maven image to build the project
FROM maven:3.9.6-eclipse-temurin-21 as build

# Set the working directory inside the container
# Set the working directory inside the container
WORKDIR /app

# Copy the pom.xml and download dependencies first for faster builds
COPY pom.xml .

# Copy and run the download script to get the LookseeCore JAR
COPY scripts/download-core.sh ./scripts/download-core.sh
RUN chmod +x ./scripts/download-core.sh
RUN bash ./scripts/download-core.sh

# Copy the rest of the project source code
COPY src ./src

# Build the application
RUN mvn clean install -DskipTests

# Use a smaller JDK image to run the app
FROM eclipse-temurin:21-jre

# Copy the built JAR file from the previous stage
COPY --from=build /app/target/*.jar app.jar

# Expose the port your app runs on (default Spring Boot is 8080)
EXPOSE 443
EXPOSE 80

# Spring profile environment variable
# Default: development (permissive CORS - allows all origins)
# Override in IaC by setting SPRING_PROFILES_ACTIVE environment variable
# Valid values: development, prod, production
#   - development: permissive CORS (allows all origins)
#   - prod/production: strict CORS (only configured origins)
ENV SPRING_PROFILES_ACTIVE=development

# Spring Boot automatically reads SPRING_PROFILES_ACTIVE environment variable
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom", "-Xms800M", "-jar", "app.jar"]



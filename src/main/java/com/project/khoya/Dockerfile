    # Use a base image with OpenJDK 17
    FROM openjdk:17-jdk-slim as build

    # Install Maven (this step is necessary because the base image doesn't have Maven)
    RUN apt-get update && apt-get install -y maven

    # Set the working directory inside the container
    WORKDIR /app

    # Copy the pom.xml to the container and download dependencies
    COPY pom.xml .

    # Cache the dependencies to speed up future builds
    RUN mvn dependency:go-offline

    # Copy the source code to the container
    COPY src /app/src

    # Build the Spring Boot application (skip tests for now)
    RUN mvn clean package -DskipTests

    # Use a smaller base image for the final stage
    FROM openjdk:17-jdk-slim

    # Set the working directory
    WORKDIR /app

    # Copy the JAR file from the build stage
    COPY --from=build /app/target/khoya-0.0.1-SNAPSHOT.jar /app/khoya.jar

    # Expose the default port (8080) for the application
    EXPOSE 8080

    # Command to run the JAR file
    CMD ["java", "-jar", "khoya.jar"]

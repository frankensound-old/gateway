# Using multi-stage build
FROM gradle:7-jdk11 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle buildFatJar --no-daemon

# Use image
FROM openjdk:11
# Expose application port
EXPOSE 8080:8080
# Create directory
RUN mkdir /app
# Copy files
COPY --from=build /home/gradle/src/build/libs/*.jar /app/app.jar
# Start the application
ENTRYPOINT ["java","-jar","/app/app.jar"]
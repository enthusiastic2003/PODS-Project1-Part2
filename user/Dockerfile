FROM eclipse-temurin:17

# Copy the entire marketplace folder (including the code, pom.xml, etc.) into /marketplace in the container
COPY . /user-service/


# Build the project using Maven
RUN cd user-service && ./mvnw package

# Define the entry point for the application
ENTRYPOINT ["java", "-jar", "/user-service/target/user-0.0.1-SNAPSHOT.jar"]

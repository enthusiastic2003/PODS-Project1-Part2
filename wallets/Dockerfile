FROM eclipse-temurin:17

# Copy the entire marketplace folder (including the code, pom.xml, etc.) into /marketplace in the container
COPY . /wallet-service/

# Build the project using Maven
RUN cd /wallet-service/ && ./mvnw package

# Define the entry point for the application
ENTRYPOINT ["java", "-jar", "/wallet-service/target/wallets-0.0.1-SNAPSHOT.jar"]

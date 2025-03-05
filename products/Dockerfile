FROM eclipse-temurin:17

# Copy the entire marketplace folder (including the code, pom.xml, etc.) into /marketplace in the container
COPY . /marketplace/

# Copy the products.csv file into the container (if needed)
COPY ./products.csv /

# Build the project using Maven
RUN cd /marketplace && ./mvnw package

# Define the entry point for the application
ENTRYPOINT ["java", "-jar", "/marketplace/target/products-0.0.1-SNAPSHOT.jar"]

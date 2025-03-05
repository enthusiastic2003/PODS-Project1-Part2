package com.sirjanhansda.pods.products;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

/**
 * This component is responsible for executing tasks after the Spring Boot application has started. 
 * It ensures that products are loaded from a specified CSV file during the application's startup.
 * The class is annotated with @Component, which marks it as a Spring-managed bean, 
 * and it will be automatically detected by Spring's component scanning.
 * 
 * Dependencies:
 * - csvProdReader: A service component that is responsible for reading products from a CSV file and saving them to the database.
 * 
 * Key methods:
 * - init(): This method is annotated with @PostConstruct, which means it will be executed once the Spring context 
 *   is fully initialized and all dependencies have been injected. It triggers the loading of products from the CSV file 
 *   and prints the result of the operation to the console.
 */
@Component
public class StartUpTasks {

    // Autowires the CSVProdReader component to access its functionality
    @Autowired
    private CSVProdReader csvProdReader;

    /**
     * This method is executed after the bean is initialized, thanks to the @PostConstruct annotation.
     * It loads products from the specified CSV file and prints a success or failure message to the console.
     */
    @PostConstruct
    public void init() {
        // Load products from the CSV file and get the transaction status (true if successful, false otherwise)
        Boolean transactionStatus = csvProdReader.loadProductsFromCsv("/products.csv");

        // Print appropriate message based on the transaction status
        if (!transactionStatus) {
            System.out.println("No products were found in CSV/ Read Error");
        } else {
            System.out.println("Successfully read products from CSV");
        }
    }
}

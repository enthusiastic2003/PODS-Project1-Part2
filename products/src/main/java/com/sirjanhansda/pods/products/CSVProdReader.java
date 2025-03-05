package com.sirjanhansda.pods.products;

import com.opencsv.CSVReader;
import com.sirjanhansda.pods.products.model.Product;
import com.sirjanhansda.pods.products.proddb.ProdDb;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is responsible for reading product data from a CSV file and saving it into the database.
 * It implements CommandLineRunner, which means that it will run automatically when the Spring Boot application starts,
 * and the file path for the CSV file is provided as a command-line argument.
 * 
 * Dependencies:
 * - prodDb: A repository for interacting with the database where product data will be stored.
 * 
 * Key methods:
 * - run(String... args): This method is executed when the Spring Boot application starts. It checks for a valid
 *   file path argument and calls `loadProductsFromCsv` to process the CSV file.
 * - loadProductsFromCsv(String filename): This method loads product data from a specified CSV file. It reads each line
 *   of the CSV, parses the product data, and adds the products to a list. After processing all lines, the products are
 *   saved into the database using `prodDb.saveAll(products)`.
 */
@Component
public class CSVProdReader implements CommandLineRunner {

    @Autowired
    private ProdDb prodDb; // Dependency injection for ProdDb repository

    /**
     * This method is executed when the Spring Boot application starts.
     * It checks if the command-line arguments contain a file path and calls the `loadProductsFromCsv` method.
     * 
     * @param args Command-line arguments, where the first argument should be the path to the CSV file.
     */
    @Override
    public void run(String... args) {
        if (args.length == 0) {
            System.err.println("Usage: Provide the path to the CSV file as an argument.");
            return;
        }
        String filePath = args[0]; // Get the file path from command-line arguments
        loadProductsFromCsv(filePath); // Load products from the specified CSV file
    }

    /**
     * This method loads products from a CSV file, parses each product's data, and saves it to the database.
     * It expects the CSV file to contain product information with the following columns:
     * 1. Product ID
     * 2. Product name
     * 3. Product description
     * 4. Product price
     * 5. Product stock quantity
     * 
     * @param filename The path to the CSV file.
     * @return true if products were successfully loaded and saved to the database.
     * @throws RuntimeException if there are any issues reading the CSV file, parsing the data, or saving the products.
     */
    Boolean loadProductsFromCsv(String filename) {
        List<Product> products = new ArrayList<>(); // List to hold all the products

        try (InputStream inputStream = new FileInputStream(filename); // Open the file for reading
             InputStreamReader reader = new InputStreamReader(inputStream); // Convert byte stream to character stream
             CSVReader csvReader = new CSVReader(reader)) { // Read CSV content

            String[] header = csvReader.readNext(); // Skip the CSV header row
            if (header == null) {
                throw new RuntimeException("Empty CSV file"); // Throw exception if the file is empty
            }

            String[] nextLine;
            while ((nextLine = csvReader.readNext()) != null) { // Read each line of the CSV
                try {
                    // Parse the CSV line into a Product object and add it to the products list
                    Product product = Product.builder()
                            .id(Integer.parseInt(nextLine[0])) // Product ID
                            .name(nextLine[1]) // Product name
                            .description(nextLine[2]) // Product description
                            .price(Integer.parseInt(nextLine[3])) // Product price
                            .stock_quantity(Integer.parseInt(nextLine[4])) // Product stock quantity
                            .build();
                    products.add(product); // Add the product to the list
                } catch (Exception e) {
                    throw new RuntimeException("Invalid product data: " + String.join(", ", nextLine), e);
                    // Handle exceptions in case of invalid data in the CSV line
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load products from CSV: " + e.getMessage(), e);
            // Handle any exceptions that occur while reading the file or parsing the data
        }

        if (products.isEmpty()) {
            throw new RuntimeException("No products found in CSV file"); // If no products were loaded
        }

        prodDb.saveAll(products); // Save all the products to the database
        return true; // Return true indicating success
    }
}

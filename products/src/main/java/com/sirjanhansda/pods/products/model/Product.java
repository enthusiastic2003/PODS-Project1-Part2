package com.sirjanhansda.pods.products.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Builder;
import lombok.Data;

/**
 * This class represents a product in the system. It is annotated with @Entity, making it a JPA entity
 * that will be mapped to a database table. The class contains the following fields:
 * - id (Integer): The unique identifier for the product, annotated with @Id to specify that it is the primary key.
 * - name (String): The name of the product.
 * - description (String): A brief description of the product.
 * - price (Integer): The price of the product.
 * - stock_quantity (Integer): The quantity of the product available in stock.
 *
 * The @Data annotation from Lombok automatically generates getter, setter, equals, hashcode, 
 * and toString methods for the class, reducing boilerplate code.
 * The @Builder annotation from Lombok provides a builder pattern for creating instances of the class,
 * making it easier to instantiate objects with multiple properties.
 * 
 * The class includes two constructors:
 * - The parameterized constructor initializes all fields of the product.
 * - The default constructor allows creating an empty product object.
 */
@Entity
@Data
@Builder
public class Product {

    // Unique identifier for the product (primary key)
    @Id
    private Integer id;

    // The name of the product
    private String name;

    // A brief description of the product
    private String description;

    // The price of the product
    private Integer price;

    // The quantity of the product available in stock
    private Integer stock_quantity;

    /**
     * Parameterized constructor to create a product with specific values for id, name, description, price, and stock quantity.
     */
    public Product(Integer id, String name, String description, Integer price, Integer quantity) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.stock_quantity = quantity;
        this.price = price;
    }

    /**
     * Default constructor for creating an empty product object.
     */
    public Product() {
    }
}

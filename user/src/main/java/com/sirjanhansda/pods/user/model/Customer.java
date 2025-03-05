package com.sirjanhansda.pods.user.model; // Defines the package for this entity class

import jakarta.persistence.Entity;        // Marks this class as a JPA entity (table in the database)
import jakarta.persistence.Id;
import lombok.Data;  // Lombok annotation to generate boilerplate code like toString, equals, hashCode


/**
 * Represents a Customer entity in the database.
 */
@Entity  // Marks this class as a JPA entity, meaning it corresponds to a database table
@Data    // Lombok annotation that includes @Getter, @Setter, @ToString, @EqualsAndHashCode
public class Customer {

    @Id  // Marks this field as the primary key
    private Integer id; // Unique identifier for the customer

    private String name;  // Name of the customer
    private String email; // Email address of the customer

    private Boolean discount_availed = false; // Flag indicating if the customer has availed a discount
}

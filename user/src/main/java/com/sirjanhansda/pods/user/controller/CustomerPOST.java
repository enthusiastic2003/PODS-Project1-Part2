package com.sirjanhansda.pods.user.controller; // Defines the package for the class

import lombok.Getter;  // Lombok annotation to generate getter methods automatically
import lombok.Setter;  // Lombok annotation to generate setter methods automatically

/**
 * DTO (Data Transfer Object) for handling customer POST requests.
 * This class represents the structure of the request body when creating a new customer.
 */
@Getter  // Generates getter methods for all fields at compile time
@Setter  // Generates setter methods for all fields at compile time
class CustomerPOST {
    int id;         // Unique identifier for the customer
    String name;    // Name of the customer
    String email;   // Email address of the customer
}

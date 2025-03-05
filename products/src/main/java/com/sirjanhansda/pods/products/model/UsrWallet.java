package com.sirjanhansda.pods.products.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

/**
 * This class represents a user's wallet in the system. It is annotated with @Entity, 
 * indicating that it is a JPA entity, and will be mapped to a database table.
 * The class contains the following fields:
 * - userid (Integer): The unique identifier for the user who owns the wallet. This is the primary key, 
 *   marked with the @Id annotation.
 * - balance (Integer): The current balance of the user's wallet, representing the amount of money 
 *   available in the wallet.
 *
 * The @Data annotation from Lombok generates boilerplate code such as getter and setter methods, 
 * equals, hashCode, and toString methods for the class.
 * The @Getter and @Setter annotations are also included for explicit getter and setter 
 * method generation, but @Data already handles this.
 */
@Entity
@Data
@Getter
@Setter
public class UsrWallet {

    // Unique identifier for the user who owns the wallet (primary key)
    @Id
    private Integer userid;

    // The current balance in the user's wallet
    private Integer balance;
}

package com.sirjanhansda.pods.products.controller;

import jakarta.persistence.Enumerated;
import lombok.Setter;

/**
 * This class represents the request body for updating a wallet's balance.
 * It contains the following fields:
 * - action (Action): The type of transaction to be performed on the wallet (either debit or credit).
 * - amount (Integer): The amount to be debited or credited to the wallet.
 * 
 * The action field is an enum, annotated with @Enumerated to specify that it will be stored as 
 * an enumerated value in the database (e.g., a string or integer depending on the database configuration).
 *
 * The @Setter annotation from Lombok automatically generates setter methods for the fields.
 */
@Setter
public class WalletPUTRequest {

    /**
     * Enum representing the possible actions for the wallet transaction.
     * The possible actions are:
     * - debit: Deduct the specified amount from the wallet.
     * - credit: Add the specified amount to the wallet.
     */
    public enum Action {
        debit,  // Represents a debit action (subtracting from the wallet)
        credit  // Represents a credit action (adding to the wallet)
    }

    // The action to be performed on the wallet (debit or credit)
    @Enumerated
    public Action action;

    // The amount to be debited or credited to the wallet
    public Integer amount;
}

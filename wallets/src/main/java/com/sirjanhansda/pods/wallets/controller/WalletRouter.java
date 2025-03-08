package com.sirjanhansda.pods.wallets.controller;

import com.sirjanhansda.pods.wallets.model.UsrWallet;
import com.sirjanhansda.pods.wallets.walletdb.WalletDb;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * REST Controller for managing digital wallet operations.
 * Provides endpoints for creating, reading, updating, and deleting wallet records.
 */
@RestController
@RequestMapping("/wallets")
public class WalletRouter {

    @Autowired
    private WalletDb walletDb;

    /**
     * Retrieves wallet information for a specific user.
     *
     * @param userId The unique identifier of the user
     * @return ResponseEntity containing wallet details or appropriate error response
     */
    @GetMapping("/{userId}")
    public ResponseEntity<?> getWallet(@PathVariable Integer userId) {
        List<UsrWallet> userWallets = walletDb.findUsrWalletByUser_id(userId);
        return userWallets.isEmpty()
                ? ResponseEntity.notFound().build()
                : ResponseEntity.ok(userWallets.get(0));
    }

    /**
     * Updates or creates a wallet for a specific user.
     * Handles both credit and debit operations.
     *
     * @param userId The unique identifier of the user
     * @param walletRequest The request containing action (credit/debit) and amount
     * @return ResponseEntity containing updated wallet details or error message
     */

    @Transactional
    @PutMapping("/{userId}")
    public ResponseEntity<?> updateWallet(
            @PathVariable Integer userId,
            @RequestBody WalletPUTRequest walletRequest) {

        // Get or create wallet
        getOrCreateWallet(userId);

        try {
            // Process the transaction
            String errorMessage = processTransaction(userId, walletRequest);
            if (errorMessage != null) {
                return ResponseEntity.badRequest().body(errorMessage);
            }

            // Save and return updated wallet
            List<UsrWallet> savedList = walletDb.findUsrWalletByUser_id(userId);
            UsrWallet savedWallet = savedList.get(0);
            return ResponseEntity.ok(savedWallet);

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Operation failed: " + e.getMessage());
        }
    }

    /**
     * Deletes a specific user's wallet.
     *
     * @param userId The unique identifier of the user
     * @return ResponseEntity indicating success or failure
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<?> deleteWallet(@PathVariable Integer userId) {
        List<UsrWallet> userWallets = walletDb.findUsrWalletByUser_id(userId);

        if (userWallets.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        try {
            walletDb.delete(userWallets.get(0));
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Operation failed: " + e.getMessage());
        }
    }

    /**
     * Deletes all wallet records from the system.
     *
     * @return ResponseEntity indicating success or failure
     */
    @DeleteMapping()
    public ResponseEntity<?> deleteAllWallets() {
        try {
            walletDb.deleteAll();
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Operation failed: " + e.getMessage());
        }
    }

    /**
     * Helper method to get existing wallet or create new one if not found.
     *
     * @param userId The unique identifier of the user
     * @return UsrWallet object (either existing or newly created)
     */
    private void getOrCreateWallet(Integer userId) {
        List<UsrWallet> userWallets = walletDb.findUsrWalletByUser_id(userId);

        if (userWallets.isEmpty()) {
            UsrWallet newWallet = new UsrWallet();
            newWallet.setUser_id(userId);
            newWallet.setBalance(0);
            walletDb.save(newWallet);
        }

    }

    /**
     * Helper method to process credit/debit transactions.
     *
     * @param usrId The wallet to process transaction for
     * @param request The transaction request details
     * @return Error message if transaction invalid, null if successful
     */
    private String processTransaction(Integer usrId, WalletPUTRequest request) {
        if (request.action == WalletPUTRequest.Action.debit) {
            int updatedRows = walletDb.debitIfSufficient(usrId, request.amount);
            if (updatedRows == 0) {
                return "Insufficient funds";  // Prevents race condition
            }
        } else if (request.action == WalletPUTRequest.Action.credit) {
            int updatedRows = walletDb.creditAmount(usrId, request.amount);
            if (updatedRows == 0) {
                return "Failed to update balance , amount = " + request.amount + " user id = "+ usrId;
            }
        }
        return null;
    }
}
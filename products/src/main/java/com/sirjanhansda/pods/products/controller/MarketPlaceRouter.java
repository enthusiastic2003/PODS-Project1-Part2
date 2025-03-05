package com.sirjanhansda.pods.products.controller;

// Required imports for handling HTTP requests, responses, and data models
import com.sirjanhansda.pods.products.model.*;
import com.sirjanhansda.pods.products.orderdb.OrdersDb;
import com.sirjanhansda.pods.products.proddb.ProdDb;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MarketPlaceRouter handles all marketplace-related operations including:
 * - Order management
 * - User order deletion
 * - Stock management
 * - Wallet balance restoration
 * 
 * This controller acts as a central hub for coordinating operations between:
 * - Order database
 * - Product database
 * - Wallet service
 * - Account service
 */
@RestController
@RequestMapping("/marketplace")
public class MarketPlaceRouter {

    // URLs for external services are injected from application.properties/yaml
    @Value("${account.service.url}")
    private String accountServiceUrl;

    @Value("${wallets.service.url}")
    private String walletServiceUrl;

    // RestTemplate instance for making HTTP calls to external services
    // Not using @Autowired to maintain better control over the instance
    private final RestTemplate restTemplate = new RestTemplate();

    // Database repositories for handling order and product data
    @Autowired
    private OrdersDb ordersDb;  // Handles all order-related database operations

    @Autowired
    private ProdDb prodDb;      // Handles all product-related database operations

    /**
     * Handles the deletion of all orders for a specific user.
     * This includes:
     * 1. Finding all orders for the user
     * 2. Cancelling any PLACED orders
     * 3. Calculating refund amount
     * 4. Restoring wallet balance
     * 5. Restoring product stock
     * 
     * @param userid The ID of the user whose orders need to be deleted
     * @return ResponseEntity with appropriate HTTP status:
     *         - 200 OK if orders were found and processed
     *         - 404 Not Found if no orders exist for the user
     */
    @DeleteMapping("/users/{userid}")
    public ResponseEntity<?> deleteOrderByUserId(@PathVariable("userid") Integer userid) {
        // Step 1: Retrieve all orders for the specified user
        List<Orders> ordersWithUserId = ordersDb.findOrdersByUser_id(userid);

        // If no orders exist for this user, return 404 Not Found
        if (ordersWithUserId.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        // Collections to track items and amounts that need processing
        List<OrderItem> orderItems = new ArrayList<>();
        boolean foundPlaced = false;  // Flag to track if any PLACED orders were found
        Integer totalRefundablePrice = 0;  // Running total of amount to be refunded

        // Step 2: Process each order
        for (Orders order : ordersWithUserId) {
            // Only process orders in PLACED status
            if (order.getStatus().equals(OrderStatus.PLACED)) {
                // Update order status to CANCELLED
                order.setStatus(OrderStatus.CANCELLED);
                ordersDb.save(order);

                // Add order amount to refund total
                totalRefundablePrice += order.getTotal_price();
                
                // Add items to list for stock restoration
                orderItems.addAll(order.getItems());
                foundPlaced = true;
            }
        }

        // Step 3: Attempt to restore balance to user's wallet
        boolean restoreDone = restoreBalance(userid, totalRefundablePrice);
        // Log the result of the wallet restoration attempt
        if (restoreDone) {
            System.out.println("Restore done");  // Consider replacing with proper logging
        } else {
            System.out.println("Restore failed"); // Consider replacing with proper logging
        }

        // Step 4: Restore stock and return appropriate response
        if (foundPlaced) {
            restoreStock(orderItems);  // Restore stock for all cancelled items
            return ResponseEntity.ok().build();
        } else {
            // Return 404 if no PLACED orders were found
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Deletes all orders in the system that are in PLACED status.
     * This method:
     * 1. Retrieves all orders
     * 2. Processes refunds for each user
     * 3. Restores product stock
     * 
     * @return ResponseEntity with HTTP status 200 OK
     */
    @DeleteMapping
    public ResponseEntity<?> deleteAllOrders() {
        // Step 1: Retrieve all orders from the database
        List<Orders> allOrders = ordersDb.findAll();
        
        // If no orders exist, return success immediately
        if (allOrders.isEmpty()) {
            return ResponseEntity.ok().build();
        }

        // Map to track refundable amounts per user
        Map<Integer, Integer> refundableAmounts = new HashMap<>();
        // List to track items that need stock restoration
        List<OrderItem> orderItems = new ArrayList<>();

        // Step 2: Process each order
        for (Orders order : allOrders) {
            if (order.getStatus().equals(OrderStatus.PLACED)) {
                // Update order status to CANCELLED
                order.setStatus(OrderStatus.CANCELLED);
                ordersDb.save(order);

                // Add refund amount to user's total
                // getOrDefault handles cases where this is the first order for the user
                refundableAmounts.put(
                    order.getUser_id(),
                    refundableAmounts.getOrDefault(order.getUser_id(), 0) + order.getTotal_price()
                );

                // Add items to list for stock restoration
                orderItems.addAll(order.getItems());
            }
        }

        // Step 3: Process refunds for each user
        for (Map.Entry<Integer, Integer> entry : refundableAmounts.entrySet()) {
            // Attempt to restore balance for each user
            boolean restoreDone = restoreBalance(entry.getKey(), entry.getValue());
            // Log the result of each restoration attempt
            System.out.println("Restore for user " + entry.getKey() + 
                             (restoreDone ? " done" : " failed"));
        }

        // Step 4: Restore stock levels for all cancelled items
        restoreStock(orderItems);
        return ResponseEntity.ok().build();
    }

    /**
     * Makes an HTTP call to the wallet service to restore a user's balance.
     * 
     * @param userid ID of the user receiving the refund
     * @param price Amount to be credited to the user's wallet
     * @return boolean indicating if the wallet update was successful
     * @throws RuntimeException if the wallet service call fails
     */
    private boolean restoreBalance(Integer userid, Integer price) {
        // Create wallet update request
        WalletPUTRequest walletPutRequest = new WalletPUTRequest();
        walletPutRequest.setAmount(price);
        walletPutRequest.setAction(WalletPUTRequest.Action.credit);

        try {
            // Make HTTP PUT request to wallet service
            ResponseEntity<?> response = restTemplate.exchange(
                walletServiceUrl + "/wallets/" + userid,  // Target URL
                HttpMethod.PUT,                           // HTTP method
                new HttpEntity<>(walletPutRequest),      // Request body
                String.class                             // Response type
            );
            // Return true only if response status is OK
            return response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            // Consider more specific exception handling
            throw new RuntimeException(e);
        }
    }

    /**
     * Restores product stock levels for cancelled orders.
     * For each item:
     * 1. Retrieves the product
     * 2. Updates its stock quantity
     * 3. Saves the updated product
     * 
     * @param restoreItems List of OrderItems containing products and quantities
     */
    private void restoreStock(List<OrderItem> restoreItems) {
        for (OrderItem restoreItem : restoreItems) {
            // Extract quantity and product ID from the order item
            Integer quantity = restoreItem.getQuantity();
            Integer prodId = restoreItem.getProduct_id();

            // Retrieve product from database
            List<Product> products = prodDb.findProductById(prodId);
            // Get the first (and should be only) product
            Product product = products.get(0);
            
            // Update stock quantity by adding back the cancelled amount
            product.setStock_quantity(product.getStock_quantity() + quantity);
            // Save updated product back to database
            prodDb.save(product);
        }
    }
}
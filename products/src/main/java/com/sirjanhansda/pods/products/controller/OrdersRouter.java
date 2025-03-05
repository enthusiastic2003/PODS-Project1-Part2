package com.sirjanhansda.pods.products.controller;

// Import required dependencies for handling HTTP requests, database operations, and model classes
import com.sirjanhansda.pods.products.model.*;
import com.sirjanhansda.pods.products.orderdb.OrdersDb;
import com.sirjanhansda.pods.products.proddb.ProdDb;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * OrdersRouter handles all order-related operations in the system.
 * This includes:
 * - Creating new orders
 * - Retrieving order information
 * - Updating order status
 * - Cancelling orders
 * - Managing stock levels
 * - Handling wallet transactions
 * - Processing customer discounts
 */
@RestController
@RequestMapping("/orders")
public class OrdersRouter {

    // Inject dependencies for database operations
    @Autowired
    private OrdersDb ordersDb;    // Repository for order-related operations

    @Autowired
    private ProdDb prodDb;        // Repository for product-related operations

    // Configuration values for external service URLs
    @Value("${account.service.url}")
    private String accountServiceUrl;  // URL for account service

    @Value("${wallets.service.url}")
    private String walletServiceUrl;   // URL for wallet service

    // RestTemplate instance for making HTTP calls to external services
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Creates a new order in the system.
     * Process flow:
     * 1. Validate user exists and get details
     * 2. Validate order quantities
     * 3. Calculate total cost with discounts
     * 4. Check wallet balance
     * 5. Process payment
     * 6. Update stock levels
     * 7. Update discount status
     * 8. Create and save order
     *
     * @param prodPOSTRequest Order request containing user ID and items
     * @return ResponseEntity with created order or error message
     */
    @PostMapping
    public ResponseEntity<?> takeOrder(@RequestBody ProdPOSTRequest prodPOSTRequest) {
        Integer userId = prodPOSTRequest.getUser_id();

        // Step 1: Verify user exists and get their details
        ResponseEntity<Customer> customerResponse = getCustomerDetails(userId);
        if (!customerResponse.getStatusCode().is2xxSuccessful()) {
            System.out.println("Cannot get user");
            return ResponseEntity.badRequest().body(customerResponse);
        }

        // Step 2: Validate quantities in order
        for(ItemFormat item: prodPOSTRequest.getItems()) {
            if(item.getQuantity() <= 0) {
                return ResponseEntity.badRequest().body("Quantity should be greater than 0");
            }
        }

        // Step 3: Calculate total cost including any applicable discounts
        double totalCost = calculateTotalCost(prodPOSTRequest, customerResponse.getBody());
        if (totalCost < 0) {
            return ResponseEntity.badRequest().body("Product not found or stock insufficient");
        }

        // Step 4: Check user's wallet balance
        ResponseEntity<UsrWallet> walletResponse = getWalletDetails(userId);
        if (!walletResponse.getStatusCode().is2xxSuccessful()) {
            return ResponseEntity.badRequest().body(walletResponse);
        }

        UsrWallet usrWallet = walletResponse.getBody();
        if (usrWallet.getBalance() < totalCost) {
            return ResponseEntity.badRequest().body("Not enough money");
        }

        // Step 5: Process payment by debiting wallet
        boolean debitSuccess = debitAmountFromWallet(userId, totalCost);
        if (!debitSuccess) {
            return ResponseEntity.badRequest().body("Failed to process payment");
        }

        // Step 6: Update product stock levels
        boolean stockUpdated = updateStockLevels(prodPOSTRequest);
        if (!stockUpdated) {
            return ResponseEntity.badRequest().body("Failed to update stock levels");
        }

        // Step 7: Update customer's discount status
        boolean discountUpdated = updateDiscountStatus(prodPOSTRequest, 
            Objects.requireNonNull(customerResponse.getBody()));
        if (!discountUpdated) {
            return ResponseEntity.badRequest().body("Failed to update discount status");
        }

        // Step 8: Create and save the order
        Orders ord = createOrder(prodPOSTRequest, userId, totalCost);

        return ResponseEntity.status(HttpStatus.CREATED).body(ord);
    }

    /**
     * Retrieves a specific order by its ID.
     *
     * @param orderid The ID of the order to retrieve
     * @return ResponseEntity containing the order or 404 if not found
     */
    @GetMapping("/{orderid}")
    public ResponseEntity<?> getOrders(@PathVariable Integer orderid) {
        List<Orders> ordersWithOrderId = ordersDb.findOrdersByOrder_id(orderid);

        if (ordersWithOrderId.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok().body(ordersWithOrderId.get(0));
    }

    /**
     * Retrieves all orders for a specific user.
     *
     * @param userId The ID of the user whose orders to retrieve
     * @return ResponseEntity containing list of user's orders
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<?> getOrdersByUserId(@PathVariable Integer userId) {
        List<Orders> ordersWithUserId = ordersDb.findOrdersByUser_id(userId);
        return ResponseEntity.ok().body(ordersWithUserId);
    }

    /**
     * Updates the status of an order.
     * Currently only supports updating to DELIVERED status.
     *
     * @param orderId Order ID to update
     * @param orderPUTStatus New status information
     * @return ResponseEntity with success/failure message
     */
    @PutMapping("/{orderId}")
    public ResponseEntity<?> setOrderStatus(@PathVariable Integer orderId, 
                                          @RequestBody OrderPUTStatus orderPUTStatus) {
        // Verify the order ID matches the request
        Integer orderIdReq = orderPUTStatus.getOrder_id();
        if(!Objects.equals(orderIdReq, orderId)) {
            return ResponseEntity.badRequest()
                .body("Requested order id is not equal to requested order id");
        }

        // Only DELIVERED status is currently supported
        if(orderPUTStatus.getStatus() != OrderStatus.DELIVERED) {
            return ResponseEntity.badRequest().body("Order not delivered");
        }

        // Fetch and validate the order
        List<Orders> ordersWithId = ordersDb.findOrdersByOrder_id(orderId);
        if (ordersWithId.isEmpty()) {
            return ResponseEntity.badRequest().body("Order not found");
        }

        Orders order = ordersWithId.get(0);

        // Can only update PLACED orders
        if(order.getStatus() != OrderStatus.PLACED) {
            return ResponseEntity.badRequest().body("Order is not placed");
        }

        // Update and save the order
        order.setStatus(OrderStatus.DELIVERED);
        ordersDb.save(order);

        return ResponseEntity.ok().build();
    }

    /**
     * Cancels an order and handles related operations:
     * 1. Refund to wallet
     * 2. Restore product stock
     *
     * @param orderid ID of order to cancel
     * @return ResponseEntity with success/failure message
     */
    @DeleteMapping("/{orderid}")
    public ResponseEntity<?> deleteOrder(@PathVariable Integer orderid) {
        // Fetch and validate the order
        List<Orders> ordersWithId = ordersDb.findOrdersByOrder_id(orderid);
        if (ordersWithId.isEmpty()) {
            return ResponseEntity.badRequest().body("Order not found");
        }

        Orders order = ordersWithId.get(0);

        // Can only cancel PLACED orders
        if(order.getStatus() != OrderStatus.PLACED) {
            return ResponseEntity.badRequest().body("Order is not placed");
        }

        // Update order status to CANCELLED
        order.setStatus(OrderStatus.CANCELLED);

        // Store information needed for refund and stock restoration
        Integer returnMoney = order.getTotal_price();
        List<OrderItem> returnItems = order.getItems();
        Integer returnUserId = order.getUser_id();
        ordersDb.save(order);

        // Process refund to wallet
        boolean balrestoreSuccess = restoreBalance(returnUserId, returnMoney);
        if (!balrestoreSuccess) {
            return ResponseEntity.badRequest().body("Failed to restore balance");
        }

        // Restore product stock levels
        restoreStock(returnItems);

        return ResponseEntity.ok().build();
    }

    /**
     * Retrieves all orders in the system.
     *
     * @return ResponseEntity containing list of all orders
     */
    @GetMapping()
    public ResponseEntity<?> getAllOrders() {
        return ResponseEntity.ok().body(ordersDb.findAll());
    }

/**
     * Restores balance to user's wallet after order cancellation.
     *
     * @param userid User ID to credit
     * @param price Amount to restore
     * @return boolean indicating success/failure
     */
    private boolean restoreBalance(Integer userid, Integer price) {
        // Prepare wallet update request
        WalletPUTRequest walletPutRequest = new WalletPUTRequest();
        walletPutRequest.setAmount(price);
        walletPutRequest.setAction(WalletPUTRequest.Action.credit);

        ResponseEntity<?> response;

        try {
            // Make API call to wallet service
            response = restTemplate.exchange(
                walletServiceUrl + "/wallets/" + userid, 
                HttpMethod.PUT, 
                new HttpEntity<>(walletPutRequest), 
                String.class
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return response.getStatusCode() == HttpStatus.OK;
    }

    /**
     * Restores product stock levels after order cancellation.
     *
     * @param restoreItems List of items whose stock to restore
     */
    private void restoreStock(List<OrderItem> restoreItems) {
        for (OrderItem restoreItem : restoreItems) {
            Integer quantity = restoreItem.getQuantity();
            Integer prodId = restoreItem.getProduct_id();

            // Fetch product and update stock
            List<Product> products = prodDb.findProductById(prodId);
            Product product = products.get(0);
            product.setStock_quantity(product.getStock_quantity() + quantity);
            prodDb.save(product);
        }
    }

    /**
     * Retrieves customer details from account service.
     *
     * @param userId ID of customer to retrieve
     * @return ResponseEntity containing customer details or error
     */
    private ResponseEntity<Customer> getCustomerDetails(Integer userId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            HttpEntity<?> requestEntity = new HttpEntity<>(headers);

            return restTemplate.exchange(
                accountServiceUrl + "/users/" + userId,
                HttpMethod.GET,
                requestEntity,
                Customer.class
            );
        } catch (HttpClientErrorException.NotFound e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Calculates total cost of order including discounts.
     *
     * @param prodPOSTRequest Order request containing items
     * @param customer Customer making the order
     * @return total cost or -1 if validation fails
     */
    private double calculateTotalCost(ProdPOSTRequest prodPOSTRequest, Customer customer) {
        double totalCost = 0;
        for (ItemFormat item : prodPOSTRequest.getItems()) {
            // Validate product exists and has sufficient stock
            List<Product> products = prodDb.findProductById(item.getProduct_id());
            if (products.isEmpty()) {
                return -1; // Product not found
            }

            Product product = products.get(0);
            if (product.getStock_quantity() < item.getQuantity()) {
                return -1; // Insufficient stock
            }

            // Apply 10% discount if customer hasn't used their discount yet
            double discountFactor = customer.getDiscount_availed() ? 1.0 : 0.9;
            totalCost += discountFactor * product.getPrice() * item.getQuantity();
        }
        return totalCost;
    }

    /**
     * Retrieves wallet details for a user.
     *
     * @param userId ID of user whose wallet to check
     * @return ResponseEntity containing wallet details or error
     */
    private ResponseEntity<UsrWallet> getWalletDetails(Integer userId) {
        try {
            return restTemplate.getForEntity(
                walletServiceUrl + "/wallets/" + userId, 
                UsrWallet.class
            );
        } catch (HttpClientErrorException.NotFound e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Debits amount from user's wallet for order payment.
     *
     * @param userId User to debit
     * @param amount Amount to debit
     * @return boolean indicating success/failure
     */
    private boolean debitAmountFromWallet(Integer userId, double amount) {
        try {
            String url = walletServiceUrl + "/wallets/" + userId;

            WalletPUTRequest request = new WalletPUTRequest();
            request.setAction(WalletPUTRequest.Action.debit);
            request.setAmount((int) amount);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<WalletPUTRequest> entity = new HttpEntity<>(request, headers);
            restTemplate.exchange(url, HttpMethod.PUT, entity, String.class);
            return true;
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

    /**
     * Updates product stock levels after successful order.
     *
     * @param prodPOSTRequest Order request containing items
     * @return boolean indicating success/failure
     */
    private boolean updateStockLevels(ProdPOSTRequest prodPOSTRequest) {
        for (ItemFormat item : prodPOSTRequest.getItems()) {
            List<Product> products = prodDb.findProductById(item.getProduct_id());
            Product product = products.get(0);
            product.setStock_quantity(product.getStock_quantity() - item.getQuantity());
            prodDb.save(product);
        }
        return true;
    }


/**
     * Updates the discount status for a customer after they use their discount.
     * This method is called after a successful order placement where a discount was applied.
     * It communicates with the account service to mark the customer's discount as used.
     *
     * @param prodPOSTRequest The original order request containing user information
     * @param customer The customer object with current discount status
     * @return boolean true if update was successful, false if it failed
     */
    private boolean updateDiscountStatus(ProdPOSTRequest prodPOSTRequest, Customer customer) {
        Integer userId = prodPOSTRequest.getUser_id();

        // Only update if customer hasn't used their discount yet
        if (!customer.getDiscount_availed()) {
            // Prepare headers for the HTTP request
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // Create request entity with 'true' to indicate discount was used
            HttpEntity<Boolean> requestEntity = new HttpEntity<>(true, headers);

            // Variable declaration for response (unused but shows intent)
            ResponseEntity<?> updateDiscount;

            try {
                // Make API call to account service to update discount status
                restTemplate.put(accountServiceUrl + "/users/" + userId, requestEntity);
            } catch (HttpClientErrorException.NotFound e) {
                // Log error and return false if user not found
                System.out.println(e.getMessage());
                return false;
            }
        }

        // Return true if no update was needed or update was successful
        return true;
    }

    /**
     * Creates a new order in the system with all related items.
     * This method is annotated with @Transactional to ensure all database operations
     * (order creation and item associations) are completed atomically.
     *
     * Process:
     * 1. Creates new order with basic information
     * 2. Creates order items and associates them with the order
     * 3. Saves the complete order with items to the database
     *
     * @param prodPOSTRequest Original order request containing items
     * @param userId ID of user placing the order
     * @param totalCost Total cost of the order (including any discounts)
     * @return Orders The created and saved order object
     */
    @Transactional
    public Orders createOrder(ProdPOSTRequest prodPOSTRequest, Integer userId, double totalCost) {
        // Create new order object and set basic properties
        Orders order = new Orders();
        order.setUser_id(userId);
        order.setStatus(OrderStatus.PLACED);  // Initial status for new orders
        order.setTotal_price((int) totalCost);  // Cast to int as price is stored as integer

        // Create list to hold order items
        List<OrderItem> orderItems = new ArrayList<>();

        // Process each item in the order request
        for(ItemFormat orderItem: prodPOSTRequest.getItems()) {
            // Create new OrderItem object for each requested item
            OrderItem orderItem1 = new OrderItem();
            orderItem1.setQuantity(orderItem.getQuantity());
            orderItem1.setProduct_id(orderItem.getProduct_id());
            
            // Establish bidirectional relationship between order and items
            orderItem1.setOrder(order);  // This is crucial for maintaining referential integrity
            
            // Add item to the list
            orderItems.add(orderItem1);
        }

        // Set all items to the order
        order.setItems(orderItems);

        // Save the complete order with all items to the database
        ordersDb.save(order);

        return order;
    }
}
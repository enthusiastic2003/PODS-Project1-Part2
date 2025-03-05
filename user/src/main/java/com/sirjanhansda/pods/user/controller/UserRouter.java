package com.sirjanhansda.pods.user.controller;

import com.sirjanhansda.pods.user.model.Customer;
import com.sirjanhansda.pods.user.userdb.UserDb;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * REST Controller for managing user operations in the PODS system.
 * Provides endpoints for CRUD operations on customer data and manages
 * interactions with marketplace and wallet services.
 */
@RestController
@RequestMapping("/users")
public class UserRouter {

    @Autowired
    private UserDb userDb;

    // Service endpoint configurations
    @Value("${service.marketplace.address}")
    private String marketplaceAddress;

    @Value("${service.wallets.address}")
    private String walletAddress;

    // RestTemplate for making HTTP requests to other services
    final RestTemplate restTemplate = new RestTemplate();

    /**
     * Creates a new user in the system.
     *
     * @param customer CustomerPOST object containing user details
     * @return ResponseEntity with created customer or error status
     */
    @PostMapping
    public ResponseEntity<?> addUser(@RequestBody final CustomerPOST customer) {
        String custEmail = customer.getEmail();

        // Check for existing user with same email
        List<Customer> existingCustomer = userDb.findCustomerByEmail(custEmail);

        if (existingCustomer.isEmpty()) {
            // Create new customer object and set initial values
            Customer newCustomer = new Customer();
            newCustomer.setId(customer.getId());
            newCustomer.setEmail(customer.getEmail());
            newCustomer.setName(customer.getName());
            newCustomer.setDiscount_availed(false);
            userDb.save(newCustomer);
            return ResponseEntity.status(HttpStatus.CREATED).body(newCustomer);
        } else {
            // Return 400 Bad Request if email already exists
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * Retrieves user information by ID.
     *
     * @param usrid User ID to lookup
     * @return ResponseEntity containing user data or 404 if not found
     */
    @GetMapping("/{usrid}")
    public ResponseEntity<?> getUser(@PathVariable final Integer usrid) {
        List<Customer> customerLists = userDb.findCustomerById(usrid);

        if (customerLists.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(customerLists.get(0));
    }

    /**
     * Updates the discount status for a specific user.
     *
     * @param usrid User ID to update
     * @param discountStatus New discount status
     * @return ResponseEntity with operation status
     */
    @PutMapping("/{usrid}")
    public ResponseEntity<?> updateUserDiscountStatus(
            @PathVariable final Integer usrid,
            @RequestBody final Boolean discountStatus) {

        List<Customer> customerLists = userDb.findCustomerById(usrid);

        if (customerLists.isEmpty()) {
            return ResponseEntity.notFound().build();
        } else {
            System.out.println(discountStatus);
            Customer customer = customerLists.get(0);
            customer.setDiscount_availed(discountStatus);

            try {
                userDb.save(customer);
            } catch (Exception e) {
                return ResponseEntity.internalServerError().body(e.getMessage());
            }

            return ResponseEntity.ok().build();
        }
    }

    /**
     * Deletes a specific user and their associated marketplace and wallet data.
     *
     * @param usrid User ID to delete
     * @return ResponseEntity with operation status
     */
    @DeleteMapping("/{usrid}")
    public ResponseEntity<?> deleteUser(@PathVariable final Integer usrid) {
        List<Customer> customerLists = userDb.findCustomerById(usrid);

        if (customerLists.isEmpty()) {
            return ResponseEntity.notFound().build();
        } else {
            Customer customer = customerLists.get(0);

            System.out.println(marketplaceAddress  +"/users/"+  usrid);
            System.out.println(walletAddress +"/"+  usrid);

            // Attempt to delete associated marketplace and wallet data
            // try {
            //     restTemplate.delete(marketplaceAddress +"/users/"+  usrid);

                
            // } catch (Exception e) {
            //     System.out.println("[WARN] Failed to delete  orders]");
            // }

            try{
                restTemplate.delete(walletAddress + "/"+usrid);
            }
            catch (Exception e)
            {
                System.out.println("[WARN] Failed to delete customer wallet");
            }

            // Delete user from database
            try {
                userDb.delete(customer);
            } catch (Exception e) {
                return ResponseEntity.internalServerError().body(e.getMessage());
            }

            return ResponseEntity.ok().build();
        }
    }

    /**
     * Deletes all users and their associated marketplace and wallet data.
     * This is a destructive operation that removes all customer records.
     *
     * @return ResponseEntity with operation status
     */
    @DeleteMapping
    public ResponseEntity<?> deleteAllUsers() {
        try {
            List<Customer> allCustomers = userDb.findAll();

            // Attempt to delete associated data for each customer
//            for (Customer customer : allCustomers) {
//                try {
//                    restTemplate.delete(marketplaceAddress+"/"+customer.getId());
//                    restTemplate.delete(walletAddress+"/"+customer.getId());
//                } catch (Exception e) {
//                    System.out.println("[WARN] Failed to delete customer wallet or orders]: " + e.getMessage());
//                }
//            }

            restTemplate.delete(marketplaceAddress);
            restTemplate.delete(walletAddress);

            // Delete all users from database
            userDb.deleteAll();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }

        return ResponseEntity.ok().build();
    }
}
package com.sirjanhansda.pods.products.controller;

import com.sirjanhansda.pods.products.model.Product;
import com.sirjanhansda.pods.products.proddb.ProdDb;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * This class defines the RESTful API endpoints for interacting with product data.
 * It exposes two main endpoints:
 * - GET /products: Retrieves all products from the database.
 * - GET /products/{prodId}: Retrieves a single product by its product ID.
 * 
 * The @RestController annotation marks this class as a controller for handling HTTP requests
 * and returning responses in a RESTful manner. The @RequestMapping("/products") specifies that 
 * all routes defined in this class will be prefixed with "/products".
 *
 * The @Autowired annotation is used to inject the ProdDb service, which provides database operations 
 * related to products.
 */
@RestController
@RequestMapping("/products")
public class ProdRouter {

    // Injecting the ProdDb service to handle database interactions
    @Autowired
    private ProdDb prodDb;

    /**
     * GET /products
     * Endpoint to retrieve all products from the database.
     * 
     * @return A ResponseEntity containing the list of all products in the database.
     */
    @GetMapping()
    public ResponseEntity<?> getProducts() {
        // Fetching all products from the database using the prodDb service
        return ResponseEntity.ok(prodDb.findAll());
    }

    /**
     * GET /products/{prodId}
     * Endpoint to retrieve a single product by its product ID.
     * 
     * @param prodId The product ID to search for.
     * @return A ResponseEntity containing the product if found, 
     *         or a 404 Not Found status if the product does not exist.
     */
    @GetMapping("/{prodId}")
    public ResponseEntity<?> getProduct(@PathVariable("prodId") final Integer prodId) {
        // Searching for the product by its ID using the prodDb service
        List<Product> ProdByProdId = prodDb.findProductById(prodId);

        // If the product is not found, return a 404 Not Found response
        if (ProdByProdId.isEmpty()) {
            return ResponseEntity.notFound().build();
        } else {
            // If the product is found, return it with a 200 OK response
            return ResponseEntity.ok(ProdByProdId.get(0));
        }
    }
}

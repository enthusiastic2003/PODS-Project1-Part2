package com.sirjanhansda.pods.products.controller;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

/**
 * This class represents the request body for creating or updating a product-related transaction.
 * It contains the following fields:
 * - user_id (Integer): The unique identifier of the user initiating the request.
 * - items (List<ItemFormat>): A list of items included in the request, where each item has a product ID and quantity.
 *
 * The @Getter and @Setter annotations from Lombok automatically generate getter and setter methods for the fields.
 */
@Getter
@Setter
public class ProdPOSTRequest {

    // The unique identifier of the user initiating the request
    Integer user_id;

    // A list of items being requested, each containing product ID and quantity
    List<ItemFormat> items;
}

/**
 * This class represents an individual item in the product request.
 * It contains the following fields:
 * - product_id (Integer): The unique identifier of the product.
 * - quantity (Integer): The quantity of the product being requested.
 *
 * The @Getter and @Setter annotations from Lombok automatically generate getter and setter methods for the fields.
 */
@Setter
@Getter
class ItemFormat {

    // The unique identifier of the product
    Integer product_id;

    // The quantity of the product being requested
    Integer quantity;
}

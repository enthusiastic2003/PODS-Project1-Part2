package com.sirjanhansda.pods.products.controller;

import com.sirjanhansda.pods.products.model.OrderStatus;
import jakarta.persistence.Enumerated;
import lombok.Getter;

/**
 * This class represents the request body for updating the status of an order.
 * It contains the following fields:
 * - order_id (Integer): The unique identifier of the order to be updated.
 * - status (OrderStatus): The new status to be assigned to the order. This field is annotated with 
 *   @Enumerated to specify that the status is an enum type, which will be mapped to its respective 
 *   database representation.
 *
 * The @Getter annotation from Lombok automatically generates getter methods for the fields.
 */
@Getter
public class OrderPUTStatus {
    // The unique identifier of the order
    private Integer order_id;

    // The new status of the order (represented as an enum)
    @Enumerated
    private OrderStatus status;
}

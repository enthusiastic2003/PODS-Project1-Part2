package com.sirjanhansda.pods.products.orderdb;

import com.sirjanhansda.pods.products.model.OrderStatus;
import com.sirjanhansda.pods.products.model.Orders;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrdersDb extends JpaRepository<Orders, Integer> {

    @Query("SELECT o FROM Orders o LEFT JOIN FETCH o.items WHERE o.order_id = :id")
    List<Orders> findOrdersByOrder_id(@Param("id") Integer id);

    @Query("SELECT o FROM Orders o LEFT JOIN FETCH o.items WHERE o.user_id = :id")
    List<Orders> findOrdersByUser_id(@Param("id") Integer id);
    List<Orders> findOrdersByStatus(OrderStatus status);
}

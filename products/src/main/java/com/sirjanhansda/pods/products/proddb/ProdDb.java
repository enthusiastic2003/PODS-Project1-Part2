package com.sirjanhansda.pods.products.proddb;


import com.sirjanhansda.pods.products.model.Product;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProdDb extends JpaRepository<Product, Integer> {

    List<Product> findProductById(Integer id);
    List<Product> findProductByName(String name);

    @Modifying
    @Transactional
    @Query("UPDATE Product p SET p.stock_quantity = p.stock_quantity - :quantity " +
            "WHERE p.id = :productId AND p.stock_quantity >= :quantity")
    int decrementStock(@Param("productId") Integer productId, @Param("quantity") Integer quantity);

    @Modifying
    @Query("UPDATE Product p SET p.stock_quantity = p.stock_quantity + :quantity WHERE p.id = :productId")
    void incrementStock(@Param("productId") Integer productId, @Param("quantity") int quantity);

}

package com.sirjanhansda.pods.products.proddb;


import com.sirjanhansda.pods.products.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProdDb extends JpaRepository<Product, Integer> {

    List<Product> findProductById(Integer id);
    List<Product> findProductByName(String name);

}

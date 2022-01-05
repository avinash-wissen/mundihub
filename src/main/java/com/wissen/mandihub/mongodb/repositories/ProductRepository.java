package com.wissen.mandihub.mongodb.repositories;

import com.wissen.mandihub.mongodb.models.Product;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ProductRepository extends MongoRepository<Product, String>
{

    Product findByName(String name);

    @Override
    Optional<Product> findById(String id);
}

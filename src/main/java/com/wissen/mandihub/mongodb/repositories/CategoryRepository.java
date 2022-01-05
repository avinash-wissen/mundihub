package com.wissen.mandihub.mongodb.repositories;

import com.wissen.mandihub.mongodb.models.Category;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CategoryRepository extends MongoRepository<Category, String>
{
    Category findByName(String categoryName);
}

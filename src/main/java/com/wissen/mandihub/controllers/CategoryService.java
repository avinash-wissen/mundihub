package com.wissen.mandihub.controllers;

import com.mongodb.client.result.UpdateResult;

import com.wissen.mandihub.mongodb.models.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import javax.validation.Valid;

import com.wissen.mandihub.mongodb.models.Product;
import com.wissen.mandihub.mongodb.repositories.CategoryRepository;

@RestController
@RequestMapping(path = "/category")
public class CategoryService
{
    @Autowired
    MongoTemplate mongoTemplate;

    @Autowired
    private CategoryRepository _categoryMongoRepository;


    //----------Retrieve Categories-------------
    @GetMapping(path = "/mongo")
    public ResponseEntity<Category> getCategoryFromMongoDB(@RequestParam(value = "name") String name)
    {
        Category categoryMongo = _categoryMongoRepository.findByName(name);
        if (categoryMongo != null)
        {
            return new ResponseEntity<>(categoryMongo, HttpStatus.OK);
        }
        System.out.println("There isn't any Category in Mongodb database with name: " + name);
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @GetMapping(path = "/all/mongo")
    public List<Category> getAllCategoriesFromMongoDB()
    {
        return _categoryMongoRepository.findAll();
    }


    //----------Create a Category---------------
    @PostMapping(path = "/mongo")
    public ResponseEntity<Category> addNewCategoryInMongoDB(@Valid @RequestBody Category category)
    {
        if (category == null || category.getName() == null || category.getName().trim().isEmpty())
        {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        Category createdCategory = _categoryMongoRepository.save(category);
        return new ResponseEntity<>(createdCategory, HttpStatus.OK);
    }


    //----------Update a Category---------------
    @PutMapping(path = "/mongo")
    public ResponseEntity<String> updateCategoryInMongoDB(@Valid @RequestBody Category category)
    {
        if (category == null || category.getId() == null || category.getName() == null || category.getName().trim().isEmpty())
        {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        Category categoryInDatabase = _categoryMongoRepository.findById(category.getId()).orElse(null);
        if (categoryInDatabase == null)
        {
            return new ResponseEntity<>("This category doesn't exists in MongoDB.", HttpStatus.NOT_FOUND);
        }

        //Update the name of the category in MongoDB Database using mongoOperation.updateFirst
        Update updateCat = new Update();
        updateCat.set("name", category.getName());
        Query queryCat = new Query(Criteria.where("_id").is(category.getId()));
        UpdateResult updateResult = mongoTemplate.updateFirst(queryCat, updateCat, Category.class);
        if (updateResult.getModifiedCount() == 1)
        {
            //After updating a category, all of the products which are in this category must be updated manually.
            Query query = new Query();
            query.addCriteria(Criteria.where("fallIntoCategories._id").is(categoryInDatabase.getId()));
            Update update = new Update().set("fallIntoCategories.$.name", category.getName());
            updateResult = mongoTemplate.updateMulti(query, update, Product.class);
            return new ResponseEntity<>("The category updated", HttpStatus.OK);
        }
        else
        {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}

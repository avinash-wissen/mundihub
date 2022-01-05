package com.wissen.mandihub.controllers;

import com.mongodb.client.result.UpdateResult;

import com.wissen.mandihub.mongodb.models.Category;
import com.wissen.mandihub.mongodb.models.EmbeddedCategory;
import com.wissen.mandihub.mongodb.models.Product;
import com.wissen.mandihub.mongodb.models.Seller;
import com.wissen.mandihub.mongodb.repositories.CategoryRepository;
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

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.EntityNotFoundException;
import javax.validation.Valid;

import com.wissen.mandihub.mongodb.repositories.ProductRepository;
import com.wissen.mandihub.mongodb.repositories.SellerRepository;

@RestController
@RequestMapping(path = "/product")
public class ProductService
{
    @Autowired
    MongoTemplate mongoTemplate;

    @Autowired
    private ProductRepository _productMongoRepository;
    @Autowired
    private SellerRepository _sellerMongoRepository;
    @Autowired
    private CategoryRepository _categoryMongoRepository;


    //----------Retrieve Products----------------
    @GetMapping(path = "/mongo")
    public ResponseEntity<Product> getProductFromMongoDB(@RequestParam(value = "name") String name)
    {
        Product productMongo = _productMongoRepository.findByName(name);
        if (productMongo != null)
        {
            return new ResponseEntity<>(productMongo, HttpStatus.OK);
        }
        System.out.println("There isn't any Product in Mongodb database with name: " + name);
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }


    @GetMapping(path = "/all/mongo")
    public List<Product> getAllProductsFromMongoDB()
    {
        return _productMongoRepository.findAll();
    }


    //----------Create a Product-----------------
    @PostMapping(path = "/mongo")
    public ResponseEntity<?> addNewProductInMongoDB(@Valid @RequestBody Product product)
    {
        Seller seller;
        HashSet<EmbeddedCategory> categories = new HashSet<>();
        try
        {
            for (EmbeddedCategory embCat : product.getFallIntoCategories())
            {
                Category category = _categoryMongoRepository.findById(embCat.getId()).orElseThrow(EntityNotFoundException::new);
                categories.add(new EmbeddedCategory(category.getId(), category.getName()));
            }
        }
        catch (EntityNotFoundException e)
        {
            return new ResponseEntity<>("One of the categories which the product falls into, doesn't exists!", HttpStatus.BAD_REQUEST);
        }
        if (categories.isEmpty())
        {
            return new ResponseEntity<>("The product must belongs to at least one category!", HttpStatus.BAD_REQUEST);
        }
        try
        {
            seller = _sellerMongoRepository.findById(product.getSeller().getId()).orElseThrow(EntityNotFoundException::new);
        }
        catch (EntityNotFoundException e)
        {
            return new ResponseEntity<>("The seller of this product doesn't exists in MongoDB!", HttpStatus.BAD_REQUEST);
        }
        Product productMongoDB = new Product(product.getName(), product.getDescription(), product.getPrice(), seller, categories);
        productMongoDB = _productMongoRepository.save(productMongoDB);
        //add a reference to this product in appropriate categories
        Update update = new Update();
        update.addToSet("productsOfCategory", productMongoDB.getId());
        List<String> catIds = productMongoDB.getFallIntoCategories().stream().map(EmbeddedCategory::getId).collect(Collectors.toList());
        Query query = new Query().addCriteria(Criteria.where("_id").in(catIds));
        UpdateResult updateResult = mongoTemplate.updateMulti(query, update, Category.class);
        System.out.println("The new product added and " + updateResult.getModifiedCount() + " categories updated.");
        return new ResponseEntity<>(productMongoDB, HttpStatus.OK);
    }


    //----------Update a Product-----------------
    @PutMapping(path = "/mongo")
    public ResponseEntity<String> updateProductInMongoDB(@Valid @RequestBody Product product)
    {
        Product productInDatabase = _productMongoRepository.findById(product.getId()).orElse(null);
        if (productInDatabase == null)
        {
            return new ResponseEntity<>("This product doesn't exists in MongoDB.", HttpStatus.NOT_FOUND);
        }
        HashSet<EmbeddedCategory> categories = new HashSet<>();
        try
        {
            for (EmbeddedCategory embCat : product.getFallIntoCategories())
            {
                Category category = _categoryMongoRepository.findById(embCat.getId()).orElseThrow(EntityNotFoundException::new);
                categories.add(new EmbeddedCategory(category.getId(), category.getName()));
            }
        }
        catch (EntityNotFoundException e)
        {
            return new ResponseEntity<>("One of the categories which the product falls into, doesn't exists!", HttpStatus.BAD_REQUEST);
        }
        if (categories.isEmpty())
        {
            return new ResponseEntity<>("The product must belongs to at least one category!", HttpStatus.BAD_REQUEST);
        }
        //Update the product by setting each property of this product in a update query.
        Update update = new Update();
        update.set("name", product.getName());
        update.set("description", product.getDescription());
        update.set("price", product.getPrice());
        update.set("image_URLs", product.getImage_URLs());
        update.set("fallIntoCategories", categories);
        Query query = new Query(Criteria.where("_id").is(product.getId()));
        UpdateResult updateResult = mongoTemplate.updateFirst(query, update, Product.class);
        if (updateResult.getModifiedCount() == 1)
        {
            productInDatabase = _productMongoRepository.findById(product.getId()).get();
            System.out.println("The \"" + productInDatabase.getName() + "\" product updated!");
            return new ResponseEntity<>("The product updated", HttpStatus.OK);
        }
        else
        {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}

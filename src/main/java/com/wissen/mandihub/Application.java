package com.wissen.mandihub;

import com.mongodb.client.result.UpdateResult;

import com.wissen.mandihub.mongodb.models.Category;
import com.wissen.mandihub.mongodb.models.Seller;
import com.wissen.mandihub.mongodb.repositories.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import com.wissen.mandihub.enums.Gender;
import com.wissen.mandihub.mongodb.models.EmbeddedCategory;
import com.wissen.mandihub.mongodb.models.Product;
import com.wissen.mandihub.mongodb.models.Profile;
import com.wissen.mandihub.mongodb.repositories.ProductRepository;
import com.wissen.mandihub.mongodb.repositories.SellerRepository;


@EnableMongoRepositories(basePackages = "com.wissen.mandihub.mongodb.repositories")
@SpringBootApplication
public class Application implements CommandLineRunner
{

    @Autowired
    MongoTemplate mongoTemplate;

    @Autowired
    private CategoryRepository _categoryMongoRepository;
    @Autowired
    private ProductRepository _productMongoRepository;
    @Autowired
    private SellerRepository _sellerMongoRepository;


    public static void main(String[] args)
    {
        SpringApplication.run(Application.class, args);
    }

    @Override
    public void run(String... strings) throws Exception
    {

        _categoryMongoRepository.deleteAll();
        _sellerMongoRepository.deleteAll();
        _productMongoRepository.deleteAll();

        ////////////////////////Test MongoDB///////////////////////////////////////////////////

        //--------------Create a seller-----------------------------------------------
        Profile profile = new Profile("Peter", "Smith", Gender.Male);
        Seller seller = new Seller("Peter's account id = 391", profile);
        _sellerMongoRepository.save(seller);

        System.out.println("__________________________________________________________________");
        System.out.println("Test MongoDB repository");
        System.out.println("Find seller(s) by first name");
        _sellerMongoRepository.findByFirstName("Peter").forEach(System.out::println);
        System.out.println("__________________________________________________________________");


        //--------------Create four different categories in MongoDB-------------------
        Category furnitureCategory = new Category("Furniture");
        Category handmadeCategory = new Category("Handmade");
        furnitureCategory = _categoryMongoRepository.save(furnitureCategory);
        handmadeCategory = _categoryMongoRepository.save(handmadeCategory);
        Category kitchenCategory = new Category("Kitchen");
        kitchenCategory = _categoryMongoRepository.save(kitchenCategory);
        Category woodCategory = new Category();
        woodCategory.setName("Wood");
        woodCategory = _categoryMongoRepository.save(woodCategory);


        //--------------Create a product in two different categories------------------
        EmbeddedCategory woodEmbedded = new EmbeddedCategory(woodCategory.getId(), woodCategory.getName());
        EmbeddedCategory handmadeEmbedded = new EmbeddedCategory(handmadeCategory.getId(), handmadeCategory.getName());
        HashSet<EmbeddedCategory> categoryList = new HashSet<>(Arrays.asList(woodEmbedded, handmadeEmbedded));
        Product desk = new Product("A Wooden Desk", "Made with thick solid reclaimed wood, Easy to Assemble", 249.99f, seller, categoryList);
        desk = _productMongoRepository.save(desk);

        Update update = new Update();
        update.addToSet("productsOfCategory", desk.getId());
        List<String> ids = desk.getFallIntoCategories().stream().map(EmbeddedCategory::getId).collect(Collectors.toList());
        Query myUpdateQuery = new Query();
        myUpdateQuery.addCriteria(Criteria.where("_id").in(ids));
        UpdateResult updateResult = mongoTemplate.updateMulti(myUpdateQuery, update, Category.class);
        System.out.println("__________________________________________________________________");
        System.out.println("The count of categories which updated after saving the desk is:  " + String.valueOf(updateResult.getMatchedCount()));


        //--------------Create a product in one category------------------------------
        EmbeddedCategory furnitureEmbedded = new EmbeddedCategory(furnitureCategory.getId(), furnitureCategory.getName());
        categoryList = new HashSet<>(Arrays.asList(furnitureEmbedded));
        Product diningChair = new Product("Antique Dining Chair",
                "This mid-century fashionable chair is quite comfortable and attractive.", 234.20f, seller, categoryList);
        diningChair = _productMongoRepository.save(diningChair);

        update = new Update();
        update.addToSet("productsOfCategory", diningChair.getId());
        ids = diningChair.getFallIntoCategories().stream().map(EmbeddedCategory::getId).collect(Collectors.toList());
        myUpdateQuery = new Query();
        myUpdateQuery.addCriteria(Criteria.where("_id").in(ids));
        updateResult = mongoTemplate.updateMulti(myUpdateQuery, update, Category.class);
        System.out.println("__________________________________________________________________");
        System.out.println("The count of categories which updated after saving the dining chair is:  " + String.valueOf(updateResult.getMatchedCount()));


        //--------------Create a product in three different categories------------------
        EmbeddedCategory kitchenEmbedded = new EmbeddedCategory(kitchenCategory.getId(), kitchenCategory.getName());
        categoryList = new HashSet<>(Arrays.asList(handmadeEmbedded, woodEmbedded, kitchenEmbedded));
        Product spoon = new Product("Bamboo Spoon", "This is more durable than traditional hardwood spoon, safe to use any cookware.", 13.11f, seller, categoryList);
        spoon = _productMongoRepository.save(spoon);

        update = new Update();
        update.addToSet("productsOfCategory", spoon.getId());
        ids = spoon.getFallIntoCategories().stream().map(EmbeddedCategory::getId).collect(Collectors.toList());
        myUpdateQuery = new Query();
        myUpdateQuery.addCriteria(Criteria.where("_id").in(ids));
        updateResult = mongoTemplate.updateMulti(myUpdateQuery, update, Category.class);
        System.out.println("__________________________________________________________________");
        System.out.println("The count of categories which updated after saving wooden spoon is:  " + String.valueOf(updateResult.getMatchedCount()));

    }
}

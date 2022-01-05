package com.wissen.mandihub.controllers;

import com.mongodb.client.result.UpdateResult;

import com.wissen.mandihub.mongodb.models.Seller;
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

import javax.persistence.EntityNotFoundException;
import javax.validation.Valid;

import com.wissen.mandihub.mongodb.models.Profile;
import com.wissen.mandihub.mongodb.repositories.SellerRepository;

@RestController
@RequestMapping(path = "/seller")
public class SellerService
{
    @Autowired
    MongoTemplate mongoTemplate;

    @Autowired
    private SellerRepository _sellerMongoRepository;


    //----------Retrieve Sellers----------------
    @GetMapping(path = "/mongo")
    public ResponseEntity<?> getSellersFromMongoDB(@RequestParam(value = "firstName") String firstName)
    {
        List<Seller> sellers = _sellerMongoRepository.findByFirstName(firstName);
        if (sellers.size() > 0)
        {
            System.out.println("There are " + sellers.size() + " sellers with first name " + firstName + " in MongoDB database.");
            return new ResponseEntity<>(sellers, HttpStatus.OK);
        }
        return new ResponseEntity<>("There isn't any seller with this name in MongoDB.", HttpStatus.NOT_FOUND);
    }

    @GetMapping(path = "/all/mongo")
    public List<Seller> getAllSellersFromMongoDB()
    {
        return _sellerMongoRepository.findAll();
    }


    //----------Create a Seller-----------------
    @PostMapping(path = "/mongo")
    public ResponseEntity<Seller> addNewSellerInMongoDB(@Valid @RequestBody Seller seller)
    {
        Profile profile = new Profile(seller.getProfile().getFirstName(), seller.getProfile().getLastName(), seller.getProfile().getGender());
        Seller sellerMongoDB = new Seller(seller.getAccountId(), profile);
        sellerMongoDB = _sellerMongoRepository.save(sellerMongoDB);
        return new ResponseEntity<>(sellerMongoDB, HttpStatus.OK);
    }


    //----------Update a Seller-----------------
    @PutMapping(path = "/mongo")
    public ResponseEntity<String> updateSellerInMongoDB(@Valid @RequestBody Seller seller) {
        try {
            Seller sellerInDatabase = _sellerMongoRepository.findById(seller.getId()).orElseThrow(EntityNotFoundException::new);
            Update update = new Update();
            update.set("accountId", seller.getAccountId());
            update.set("profile.firstName", seller.getProfile().getFirstName());
            update.set("profile.lastName", seller.getProfile().getLastName());
            update.set("profile.website", seller.getProfile().getWebsite());
            update.set("profile.birthday", seller.getProfile().getBirthday());
            update.set("profile.address", seller.getProfile().getAddress());
            update.set("profile.emailAddress", seller.getProfile().getEmailAddress());
            update.set("profile.gender", seller.getProfile().getGender());

            Query query = new Query(Criteria.where("_id").is(seller.getId()));
            UpdateResult updateResult = mongoTemplate.updateFirst(query, update, Seller.class);
            if (updateResult.getModifiedCount() == 1) {
                sellerInDatabase = _sellerMongoRepository.findById(seller.getId()).orElseThrow(EntityNotFoundException::new);
                System.out.println("__________________________________________________________________");
                System.out.println("The document of " + sellerInDatabase.toString() + " updated");
                return new ResponseEntity<>("The seller updated", HttpStatus.OK);
            } else {
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (EntityNotFoundException e) {
            return new ResponseEntity<>("This seller doesn't exists in MongoDB.", HttpStatus.NOT_FOUND);
        }
    }
}

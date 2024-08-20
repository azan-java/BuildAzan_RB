package com.buildazan.repo;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.buildazan.entities.Category;

@Repository
public interface CategoryRepo extends MongoRepository<Category, String>{
    
}
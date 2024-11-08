package com.buildazan.repo;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.buildazan.entities.Page;

@Repository
public interface PageRepo extends MongoRepository<Page, String>{
    public Page findPageBySlug(String slug);
}
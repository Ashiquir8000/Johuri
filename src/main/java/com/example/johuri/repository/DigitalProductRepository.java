package com.example.johuri.repository;

import com.example.johuri.entity.DigitalProduct;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface DigitalProductRepository extends MongoRepository<DigitalProduct, String> {

    List<DigitalProduct> findByCreatorId(String creatorId);
}

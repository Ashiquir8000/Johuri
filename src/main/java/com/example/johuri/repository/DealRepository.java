package com.example.johuri.repository;

import com.example.johuri.entity.Deal;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface DealRepository extends MongoRepository<Deal, String> {

    List<Deal> findByCreatorId(String creatorId);
}

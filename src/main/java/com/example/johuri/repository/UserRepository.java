package com.example.johuri.repository;

import com.example.johuri.entity.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByEmail(String email);

    Optional<User> findByEmailIgnoreCase(String email);
}

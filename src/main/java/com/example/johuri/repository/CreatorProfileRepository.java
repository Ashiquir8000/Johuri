package com.example.johuri.repository;

import com.example.johuri.entity.CreatorProfile;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface CreatorProfileRepository extends MongoRepository<CreatorProfile, String> {

    Optional<CreatorProfile> findByUserId(String userId);

    List<CreatorProfile> findByYoutubeHandle(String youtubeHandle);

    List<CreatorProfile> findByYoutubeHandleIgnoreCase(String youtubeHandle);
}

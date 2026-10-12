package com.example.johuri.repository;

import com.example.johuri.entity.CampaignDeal;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CampaignDealRepository extends MongoRepository<CampaignDeal, String> {
    Optional<CampaignDeal> findByDealId(String dealId);
    Optional<CampaignDeal> findByBkashSessionId(String bkashSessionId);
    java.util.List<CampaignDeal> findByCreatorHandleIgnoreCase(String creatorHandle);
    java.util.List<CampaignDeal> findByBrandNameIgnoreCase(String brandName);
    java.util.List<CampaignDeal> findByStatus(CampaignDeal.Status status);
    java.util.List<CampaignDeal> findByStatusIn(java.util.Collection<CampaignDeal.Status> statuses);
}


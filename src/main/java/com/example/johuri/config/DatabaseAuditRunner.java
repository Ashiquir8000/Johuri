package com.example.johuri.config;

import com.example.johuri.repository.CampaignDealRepository;
import com.mongodb.client.MongoClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class DatabaseAuditRunner implements CommandLineRunner {

    private final MongoTemplate mongoTemplate;
    private final CampaignDealRepository campaignDealRepository;
    private final MongoClient mongoClient;

    @Override
    public void run(String... args) {
        log.info("==================================================================");
        log.info("         JOHURI MONGODB PERSISTENCE & CONNECTION AUDIT            ");
        log.info("==================================================================");
        try {
            String dbName = mongoTemplate.getDb().getName();
            String clusterNodeDescription = mongoClient.getClusterDescription().getShortDescription();
            long dealCount = campaignDealRepository.count();
            long collectionsCount = mongoTemplate.getDb().listCollectionNames().into(new java.util.ArrayList<>()).size();

            log.info("[MongoDB Audit] Active Connected Database: {}", dbName);
            log.info("[MongoDB Audit] Cluster Description / Nodes: {}", clusterNodeDescription);
            log.info("[MongoDB Audit] Total Collections in DB: {}", collectionsCount);
            log.info("[MongoDB Audit] Total Documents in 'campaign_deals': {}", dealCount);
            log.info("==================================================================");
        } catch (Exception e) {
            log.error("[MongoDB Audit ERROR] Failed to connect or query MongoDB! Check URI configuration or network access.", e);
            log.error("==================================================================");
        }
    }
}


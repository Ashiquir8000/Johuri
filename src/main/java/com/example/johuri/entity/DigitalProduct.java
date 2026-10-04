package com.example.johuri.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "digital_products")
public class DigitalProduct {

    @Id
    private String id;

    @Indexed
    private String creatorId;

    private String title;
    private String fileDownloadUrl;
    private Double priceBDT;
}

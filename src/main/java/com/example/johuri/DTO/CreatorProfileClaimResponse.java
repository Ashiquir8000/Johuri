package com.example.johuri.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatorProfileClaimResponse {

    private String id;
    private String userId;
    private String youtubeHandle;
    private String verificationStatus;
}

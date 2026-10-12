package com.example.johuri.DTO;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmitProofRequest {

    @NotBlank(message = "Deal ID is required")
    private String dealId;

    @NotBlank(message = "Proof link is required")
    private String proofLink;
}


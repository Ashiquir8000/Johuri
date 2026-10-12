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
public class EscrowConfirmRequest {

    @NotBlank(message = "Deal ID is required")
    private String dealId;

    private String bkashSessionId;
    private String otp;
    private String pin;
}


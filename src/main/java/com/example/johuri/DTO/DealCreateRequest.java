package com.example.johuri.DTO;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DealCreateRequest {

    @NotBlank
    @Size(max = 100)
    private String creatorId;

    @NotBlank
    @Size(max = 200)
    private String brandName;

    @NotBlank
    @Email
    @Size(max = 254)
    private String brandEmail;

    @NotNull
    @DecimalMin(value = "0.01")
    private Double agreedAmountBDT;
}

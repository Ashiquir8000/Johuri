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
public class ReviewDeliverableRequest {
    @NotBlank(message = "Review action (APPROVE or REJECT) is required")
    private String action;

    @NotBlank(message = "Admin notes are mandatory")
    private String adminNotes;
}


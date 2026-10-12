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
public class AdminActionRequest {
    @NotBlank(message = "Admin resolution notes are mandatory")
    private String adminNotes;
}


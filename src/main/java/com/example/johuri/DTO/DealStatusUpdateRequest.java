package com.example.johuri.DTO;

import com.example.johuri.entity.Deal;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DealStatusUpdateRequest {

    @NotNull
    private Deal.Status status;
}

package com.example.johuri.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GeminiAIResponse {

    private Long fairValuationBDT;
    private String counterProposalText;
    private boolean mockResponse;
}

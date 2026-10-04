package com.example.johuri.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatorProfileClaimRequest {

    @NotBlank
    @Size(max = 100)
    private String youtubeHandle;

    @NotBlank
    @Email
    @Size(max = 254)
    private String googleAccountEmail;

    @NotBlank
    @Size(max = 150)
    private String youtubeChannelId;
}

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
public class UserRegistrationRequest {

    @NotBlank
    @Size(max = 100)
    private String name;

    @NotBlank
    @Email
    @Size(max = 254)
    private String email;
}

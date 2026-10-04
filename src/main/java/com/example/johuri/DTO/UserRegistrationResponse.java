package com.example.johuri.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRegistrationResponse {

    private String id;
    private String name;
    private String email;
    private List<String> roles;
    private Instant createdAt;
}

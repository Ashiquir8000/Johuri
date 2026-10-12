package com.example.johuri.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "users")
public class User {

    @Id
    private String id;

    @Indexed(unique = true)
    private String email;

    private String password;
    private String fullName;
    private Role role;
    private String creatorHandle;
    private Instant createdAt;

    // Backward compatibility helpers
    public String getName() {
        return fullName;
    }

    public void setName(String name) {
        this.fullName = name;
    }

    public List<String> getRoles() {
        return role != null ? List.of("ROLE_" + role.name()) : List.of("ROLE_CREATOR");
    }

    public enum Role {
        BRAND,
        CREATOR,
        ADMIN
    }
}


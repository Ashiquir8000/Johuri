package com.example.johuri.entity;

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
    private String name;
    private Role role;
    private List<String> roles;
    private String creatorHandle;
    private String connectedYouTubeChannelId;
    private Instant createdAt;

    public String getName() {
        return fullName != null ? fullName : name;
    }

    public void setName(String name) {
        this.name = name;
        if (this.fullName == null) {
            this.fullName = name;
        }
    }

    public String getFullName() {
        return fullName != null ? fullName : name;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
        this.name = fullName;
    }

    public List<String> getRoles() {
        if (roles != null && !roles.isEmpty()) return roles;
        return role != null ? List.of("ROLE_" + role.name()) : List.of("ROLE_CREATOR");
    }

    public enum Role {
        BRAND,
        CREATOR,
        ADMIN
    }
}

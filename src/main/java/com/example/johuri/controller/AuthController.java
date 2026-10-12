package com.example.johuri.controller;

import com.example.johuri.DTO.ApiResponse;
import com.example.johuri.DTO.AuthResponse;
import com.example.johuri.DTO.LoginRequest;
import com.example.johuri.DTO.RegisterRequest;
import com.example.johuri.config.JwtUtils;
import com.example.johuri.entity.CreatorProfile;
import com.example.johuri.entity.User;
import com.example.johuri.repository.CreatorProfileRepository;
import com.example.johuri.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final CreatorProfileRepository creatorProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        
        if (userRepository.findByEmailIgnoreCase(email).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error("An account with email " + email + " already exists."));
        }

        User.Role role = User.Role.CREATOR;
        if (request.getRole() != null) {
            try {
                role = User.Role.valueOf(request.getRole().trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                if (request.getRole().equalsIgnoreCase("BRAND")) {
                    role = User.Role.BRAND;
                }
            }
        }

        String creatorHandle = null;
        if (role == User.Role.CREATOR && request.getYoutubeHandle() != null && !request.getYoutubeHandle().isBlank()) {
            creatorHandle = request.getYoutubeHandle().trim();
            if (!creatorHandle.startsWith("@")) {
                creatorHandle = "@" + creatorHandle;
            }
        }

        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName().trim())
                .name(request.getFullName().trim())
                .role(role)
                .roles(List.of("ROLE_" + role.name()))
                .creatorHandle(creatorHandle)
                .createdAt(Instant.now())
                .build();

        user = userRepository.save(user);
        log.info("New user registered successfully: id={}, email={}, role={}", user.getId(), user.getEmail(), user.getRole());

        // If creator, sync or create CreatorProfile and rate card
        if (role == User.Role.CREATOR && creatorHandle != null) {
            syncCreatorProfile(user, creatorHandle, request.getRateCard());
        }

        String token = jwtUtils.generateToken(user.getEmail(), user.getRole().name(), user.getId(), user.getCreatorHandle());

        AuthResponse authResponse = AuthResponse.builder()
                .jwtToken(token)
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .creatorHandle(user.getCreatorHandle())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Account registered successfully", authResponse));
    }

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<AuthResponse>> signup(@Valid @RequestBody RegisterRequest request) {
        return register(request);
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElse(null);

        if (user == null || user.getPassword() == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Invalid email or password. Please verify your credentials."));
        }

        String token = jwtUtils.generateToken(
                user.getEmail(),
                user.getRole() != null ? user.getRole().name() : "CREATOR",
                user.getId(),
                user.getCreatorHandle()
        );

        AuthResponse authResponse = AuthResponse.builder()
                .jwtToken(token)
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName() != null ? user.getFullName() : user.getName())
                .role(user.getRole() != null ? user.getRole().name() : "CREATOR")
                .creatorHandle(user.getCreatorHandle())
                .build();

        log.info("User logged in successfully: email={}, role={}", user.getEmail(), authResponse.getRole());
        return ResponseEntity.ok(ApiResponse.success("Login successful", authResponse));
    }

    private void syncCreatorProfile(User user, String handle, Map<String, Long> rateCard) {
        try {
            List<CreatorProfile> existingProfiles = creatorProfileRepository.findByYoutubeHandleIgnoreCase(handle);
            CreatorProfile profile = existingProfiles.isEmpty() ? null : existingProfiles.get(0);

            if (profile == null) {
                profile = CreatorProfile.builder()
                        .userId(user.getId())
                        .youtubeHandle(handle)
                        .verificationStatus(CreatorProfile.VerificationStatus.VERIFIED)
                        .servicePackages(new ArrayList<>())
                        .build();
            } else {
                profile.setUserId(user.getId());
                profile.setVerificationStatus(CreatorProfile.VerificationStatus.VERIFIED);
            }

            if (rateCard != null && !rateCard.isEmpty()) {
                List<CreatorProfile.ServicePackage> packages = new ArrayList<>();
                if (rateCard.containsKey("video")) {
                    packages.add(CreatorProfile.ServicePackage.builder()
                            .title("Dedicated Video Integration")
                            .description("Full sponsored segment or dedicated brand integration video.")
                            .priceBDT(rateCard.get("video").doubleValue())
                            .build());
                }
                if (rateCard.containsKey("shorts")) {
                    packages.add(CreatorProfile.ServicePackage.builder()
                            .title("YouTube Shorts / Reel")
                            .description("High-impact vertical short video promoting brand product.")
                            .priceBDT(rateCard.get("shorts").doubleValue())
                            .build());
                }
                if (rateCard.containsKey("story")) {
                    packages.add(CreatorProfile.ServicePackage.builder()
                            .title("Community / Story Post")
                            .description("Interactive post with call-to-action link.")
                            .priceBDT(rateCard.get("story").doubleValue())
                            .build());
                }
                profile.setServicePackages(packages);
            }

            creatorProfileRepository.save(profile);
            log.info("Synced CreatorProfile for handle {} with {} packages", handle, profile.getServicePackages() != null ? profile.getServicePackages().size() : 0);
        } catch (Exception ex) {
            log.error("Failed to sync CreatorProfile for handle {}: {}", handle, ex.getMessage());
        }
    }
}


package com.example.johuri.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.Map;

@Slf4j
@Component
public class JwtUtils {

    // 256-bit secret key fallback if not provided in properties
    private static final String DEFAULT_SECRET = "johuri-ai-secure-secret-key-bangladesh-creators-marketplace-2026";
    private static final long EXPIRATION_MS = 86_400_000L * 7; // 7 days

    private final Key signingKey;

    public JwtUtils(@Value("${app.jwt.secret:}") String secret) {
        String effectiveSecret = (secret != null && secret.length() >= 32) ? secret : DEFAULT_SECRET;
        this.signingKey = Keys.hmacShaKeyFor(effectiveSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    public String generateToken(String email, String role, String userId, String creatorHandle) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + EXPIRATION_MS);

        return Jwts.builder()
                .setSubject(email)
                .claim("userId", userId)
                .claim("role", role)
                .claim("creatorHandle", creatorHandle != null ? creatorHandle : "")
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims getClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String getEmailFromToken(String token) {
        return getClaimsFromToken(token).getSubject();
    }

    public String getRoleFromToken(String token) {
        return (String) getClaimsFromToken(token).get("role");
    }

    public String getCreatorHandleFromToken(String token) {
        return (String) getClaimsFromToken(token).get("creatorHandle");
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(signingKey).build().parseClaimsJws(token);
            return true;
        } catch (Exception ex) {
            log.warn("Invalid or expired JWT token: {}", ex.getMessage());
            return false;
        }
    }
}


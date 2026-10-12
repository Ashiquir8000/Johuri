package com.example.johuri.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(withDefaults())
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz
                // Static resources & UI pages
                .requestMatchers(
                        "/", 
                        "/index.html", 
                        "/creators.html", 
                        "/creator-profile.html", 
                        "/brand-dashboard.html", 
                        "/creator-dashboard.html", 
                        "/admin-dashboard.html",
                        "/auth.html",
                        "/login.html", 
                        "/404.html",
                        "/css/**", 
                        "/js/**",
                        "/images/**",
                        "/favicon.ico"
                ).permitAll()
                // Public auth endpoints
                .requestMatchers("/api/v1/auth/**").permitAll()
                // Public Creator Marketplace & Profile telemetry read-only endpoints
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/creators/public/**").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/creators/**").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/v1/creators/sync/**").permitAll()
                // Strict Role-Based Dashboard routes
                .requestMatchers("/api/v1/creators/dashboard/**").hasAnyAuthority("ROLE_CREATOR", "ROLE_ADMIN")
                .requestMatchers("/api/v1/brand/dashboard/**").hasAuthority("ROLE_BRAND")
                .requestMatchers("/api/v1/admin/**").hasAuthority("ROLE_ADMIN")
                // AI Pitch & Evaluation requires valid JWT Authentication
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/v1/ai/pitch/**", "/api/v1/ai/**").authenticated()
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/v1/creators/*/generate-pitch").authenticated()
                // Require authentication for Escrow and Onboarding endpoints
                .requestMatchers("/api/v1/escrow/**", "/api/v1/creators/onboard", "/api/v1/creators/claim").authenticated()
                .anyRequest().permitAll()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
            
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}


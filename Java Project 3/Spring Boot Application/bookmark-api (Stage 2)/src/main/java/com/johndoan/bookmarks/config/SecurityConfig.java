package com.johndoan.bookmarks.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * THE TOKEN CONSUMER (OAuth2 Resource Server).
 *
 * Security chain #2 ({@code @Order(2)}) protects the application's own
 * endpoints. Any request without a valid {@code Authorization: Bearer <jwt>}
 * header is rejected with 401; a valid token's scopes are checked against the
 * rules below.
 *
 * Scope mapping: Spring maps a JWT scope like {@code bookmark.read} to the
 * authority {@code SCOPE_bookmark.read}, which is what {@code hasAuthority(...)}
 * expects.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    @Order(2)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // Open to everyone — no token required.
                        .requestMatchers("/public/**", "/actuator/health").permitAll()
                        // Reading bookmarks needs the read scope. We list both the
                        // bare collection path and the wildcard so the scope rule holds
                        // regardless of which path-matcher Spring uses.
                        .requestMatchers(HttpMethod.GET, "/api/bookmarks", "/api/bookmarks/**").hasAuthority("SCOPE_bookmark.read")
                        // Creating/updating/deleting needs the write scope.
                        .requestMatchers(HttpMethod.POST, "/api/bookmarks", "/api/bookmarks/**").hasAuthority("SCOPE_bookmark.write")
                        .requestMatchers(HttpMethod.PUT, "/api/bookmarks", "/api/bookmarks/**").hasAuthority("SCOPE_bookmark.write")
                        .requestMatchers(HttpMethod.DELETE, "/api/bookmarks", "/api/bookmarks/**").hasAuthority("SCOPE_bookmark.write")
                        // Anything else still requires a valid token.
                        .anyRequest().authenticated())
                // Stateless JSON API: no browser sessions, so CSRF protection is unnecessary.
                .csrf(csrf -> csrf.disable())
                // Validate JWTs using the JwtDecoder bean from AuthorizationServerConfig.
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
        return http.build();
    }
}

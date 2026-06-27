package com.johndoan.bookmarks.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Application security: two more filter chains beyond the Authorization Server's.
 *
 *   chain #2 — the REST API (/api/**): stateless, protected by JWT bearer tokens
 *              and scopes (the resource server).
 *   chain #3 — everything else: a browser FORM LOGIN, which is what authenticates
 *              a human when they hit /oauth2/authorize during the login flow.
 *
 * Two in-memory users are provided to log in as.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /** Chain #2: the JSON API. Bearer-token only, no sessions. */
    @Bean
    @Order(2)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/**")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/bookmarks", "/api/bookmarks/**").hasAuthority("SCOPE_bookmark.read")
                        .requestMatchers(HttpMethod.POST, "/api/bookmarks", "/api/bookmarks/**").hasAuthority("SCOPE_bookmark.write")
                        .requestMatchers(HttpMethod.PUT, "/api/bookmarks/**").hasAuthority("SCOPE_bookmark.write")
                        .requestMatchers(HttpMethod.DELETE, "/api/bookmarks/**").hasAuthority("SCOPE_bookmark.write")
                        .anyRequest().authenticated())
                .csrf(csrf -> csrf.disable())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
        return http.build();
    }

    /**
     * Chain #3: the browser side. Serves the auto-generated /login page and
     * authenticates the user session used by the authorization endpoint.
     */
    @Bean
    @Order(3)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/public/**", "/actuator/health", "/authorized").permitAll()
                        .anyRequest().authenticated())
                .formLogin(Customizer.withDefaults());
        return http.build();
    }

    /**
     * Two demo users. Passwords use the {noop} prefix (plain text) so they work
     * with the delegating password encoder below — fine for learning, never for
     * production.
     */
    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails john = User.withUsername("john")
                .password("{noop}password")
                .roles("USER")
                .build();
        UserDetails jane = User.withUsername("jane")
                .password("{noop}password")
                .roles("USER")
                .build();
        return new InMemoryUserDetailsManager(john, jane);
    }

    /**
     * A delegating encoder understands the {noop} / {bcrypt} prefixes. It is used
     * both for the user passwords above and for verifying the {noop} client secret
     * in the Authorization Server.
     *
     * Intentionally ONE shared bean: don't split this into separate encoders —
     * the Authorization Server resolves this same bean to check the
     * "{noop}bruno-secret" client secret, so replacing it with a plain
     * BCryptPasswordEncoder would break the client_credentials flow.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}

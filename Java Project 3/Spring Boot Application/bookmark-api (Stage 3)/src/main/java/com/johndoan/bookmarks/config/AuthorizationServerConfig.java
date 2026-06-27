package com.johndoan.bookmarks.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.web.SecurityFilterChain;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.UUID;

/**
 * THE TOKEN ISSUER (OAuth2 Authorization Server).
 *
 * This half of the app exposes the standard OAuth2 endpoints — most importantly
 * {@code POST /oauth2/token}, where a registered client trades its credentials
 * for a JWT access token.
 *
 * We use the simplest grant for API-to-API calls: {@code client_credentials}.
 * Bruno sends its client id + secret (HTTP Basic) and gets back a signed JWT.
 */
@Configuration
public class AuthorizationServerConfig {

    /**
     * Security chain #1 — owns the OAuth2 protocol endpoints (/oauth2/token,
     * /oauth2/jwks, /.well-known/...). {@code @Order(1)} makes it match before
     * the application's own chain in {@link SecurityConfig}.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);
        return http.build();
    }

    /**
     * The one client allowed to request tokens. In a real system this lives in
     * a database; in-memory is perfect for learning.
     *
     * {@code {noop}} means the secret is stored as plain text (no hashing) —
     * fine for a local demo, never for production.
     */
    @Bean
    public RegisteredClientRepository registeredClientRepository() {
        RegisteredClient brunoClient = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("bruno-client")
                .clientSecret("{noop}bruno-secret")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .scope("bookmark.read")
                .scope("bookmark.write")
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofHours(1))
                        .build())
                .build();
        return new InMemoryRegisteredClientRepository(brunoClient);
    }

    /**
     * The RSA key pair used to SIGN issued JWTs. Generated fresh on every
     * startup (so tokens don't survive a restart — fine for learning). The
     * public half is published at /oauth2/jwks so the resource server can
     * verify signatures.
     */
    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        KeyPair keyPair = generateRsaKey();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString())
                .build();
        JWKSet jwkSet = new JWKSet(rsaKey);
        return new ImmutableJWKSet<>(jwkSet);
    }

    /**
     * The decoder the RESOURCE SERVER uses to validate tokens. Because it is
     * built from the same {@link JWKSource} that signs them, tokens this app
     * issues are trusted by this app's own protected endpoints.
     */
    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    /** Default settings: issuer is derived from the request (http://localhost:8080). */
    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder().build();
    }

    private static KeyPair generateRsaKey() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            return keyPairGenerator.generateKeyPair();
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to generate RSA key pair", ex);
        }
    }
}

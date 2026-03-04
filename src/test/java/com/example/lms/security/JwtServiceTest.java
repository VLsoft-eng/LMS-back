package com.example.lms.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    private static final String SECRET =
            "dGVzdC1zZWNyZXQta2V5LXRoYXQtaXMtbG9uZy1lbm91Z2gtZm9yLUhTMjU2LWFsZ29yaXRobQ==";
    private static final long EXPIRATION_MS = 604_800_000L; // 7 days

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, EXPIRATION_MS);
    }

    @Test
    void should_generateNonBlankToken_whenUserDetailsProvided() {
        // Given
        UserDetails user = buildUser("user@example.com");

        // When
        String token = jwtService.generateToken(user);

        // Then
        assertThat(token).isNotBlank();
    }

    @Test
    void should_generateToken_thatContainsEmailAsClaim() {
        // Given
        UserDetails user = buildUser("user@example.com");

        // When
        String token = jwtService.generateToken(user);

        // Then
        assertThat(jwtService.extractEmail(token)).isEqualTo("user@example.com");
    }

    @Test
    void should_extractEmail_fromValidToken() {
        // Given
        String email = "test@lms.com";
        String token = jwtService.generateToken(buildUser(email));

        // When
        String extracted = jwtService.extractEmail(token);

        // Then
        assertThat(extracted).isEqualTo(email);
    }

    @Test
    void should_returnTrue_whenTokenIsValidForUser() {
        // Given
        UserDetails user = buildUser("valid@lms.com");
        String token = jwtService.generateToken(user);

        // When
        boolean result = jwtService.validateToken(token, user);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void should_returnFalse_whenTokenBelongsToDifferentUser() {
        // Given
        String token = jwtService.generateToken(buildUser("owner@lms.com"));
        UserDetails other = buildUser("other@lms.com");

        // When
        boolean result = jwtService.validateToken(token, other);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void should_returnFalse_whenTokenSignatureIsTampered() {
        // Given
        String token = jwtService.generateToken(buildUser("user@lms.com"));
        String tampered = token.substring(0, token.lastIndexOf('.') + 1) + "invalidsignature";
        UserDetails user = buildUser("user@lms.com");

        // When
        boolean result = jwtService.validateToken(tampered, user);

        // Then
        assertThat(result).isFalse();
    }

    private UserDetails buildUser(String email) {
        return User.withUsername(email)
                .password("hash")
                .authorities(Collections.emptyList())
                .build();
    }
}

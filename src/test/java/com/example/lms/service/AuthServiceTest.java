package com.example.lms.service;

import com.example.lms.dto.AuthResponse;
import com.example.lms.dto.LoginRequest;
import com.example.lms.dto.RegisterRequest;
import com.example.lms.entity.UserEntity;
import com.example.lms.exception.ConflictException;
import com.example.lms.repository.UserRepository;
import com.example.lms.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void should_registerUser_when_emailNotTaken() {
        // Given
        RegisterRequest request = new RegisterRequest(
                "Ivan", "Ivanov", "ivan@test.com", "password123", null, null);

        when(userRepository.existsByEmail("ivan@test.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$12$hashed");
        when(userRepository.save(any(UserEntity.class))).thenAnswer(inv -> {
            UserEntity u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });
        when(jwtService.generateToken(any())).thenReturn("jwt-token");

        // When
        AuthResponse response = authService.register(request);

        // Then
        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.user().email()).isEqualTo("ivan@test.com");

        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("$2a$12$hashed");
        assertThat(captor.getValue().getPasswordHash()).doesNotContain("password123");
    }

    @Test
    void should_throwConflict_when_emailAlreadyExists() {
        // Given
        RegisterRequest request = new RegisterRequest(
                "Ivan", "Ivanov", "taken@test.com", "password123", null, null);
        when(userRepository.existsByEmail("taken@test.com")).thenReturn(true);

        // When / Then
        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("taken@test.com");

        verify(userRepository, never()).save(any());
    }

    @Test
    void should_neverStorePlainPassword_when_registering() {
        // Given
        RegisterRequest request = new RegisterRequest(
                "Anna", "Smirnova", "anna@test.com", "s3cr3tPass!", null, null);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode("s3cr3tPass!")).thenReturn("$2a$12$someHash");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generateToken(any())).thenReturn("token");

        // When
        authService.register(request);

        // Then
        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).doesNotContain("s3cr3tPass!");
    }

    @Test
    void should_returnAuthResponse_when_credentialsValid() {
        // Given
        LoginRequest request = new LoginRequest("ivan@test.com", "password123");
        UserEntity user = UserEntity.builder()
                .id(UUID.randomUUID())
                .email("ivan@test.com")
                .passwordHash("$2a$12$hashed")
                .firstName("Ivan")
                .lastName("Ivanov")
                .build();

        when(userRepository.findByEmail("ivan@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "$2a$12$hashed")).thenReturn(true);
        when(jwtService.generateToken(any())).thenReturn("jwt-token");

        // When
        AuthResponse response = authService.login(request);

        // Then
        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.user().email()).isEqualTo("ivan@test.com");
    }

    @Test
    void should_throwUnauthorized_when_emailNotFound() {
        // Given
        LoginRequest request = new LoginRequest("ghost@test.com", "password123");
        when(userRepository.findByEmail("ghost@test.com")).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void should_throwUnauthorized_when_passwordIsWrong() {
        // Given
        LoginRequest request = new LoginRequest("ivan@test.com", "wrongPass");
        UserEntity user = UserEntity.builder()
                .id(UUID.randomUUID())
                .email("ivan@test.com")
                .passwordHash("$2a$12$hashed")
                .firstName("Ivan")
                .lastName("Ivanov")
                .build();

        when(userRepository.findByEmail("ivan@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPass", "$2a$12$hashed")).thenReturn(false);

        // When / Then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);
    }
}

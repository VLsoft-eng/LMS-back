package com.example.lms.service;

import com.example.lms.dto.UpdateProfileRequest;
import com.example.lms.dto.UserDto;
import com.example.lms.entity.UserEntity;
import com.example.lms.exception.ResourceNotFoundException;
import com.example.lms.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    // ─── getProfile ──────────────────────────────────────────────────────────

    @Test
    void should_returnUserDto_when_userExists() {
        // Given
        UUID id = UUID.randomUUID();
        UserEntity user = UserEntity.builder()
                .id(id)
                .firstName("Ivan")
                .lastName("Ivanov")
                .email("ivan@test.com")
                .passwordHash("hash")
                .build();
        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        // When
        UserDto result = userService.getProfile(id);

        // Then
        assertThat(result.id()).isEqualTo(id);
        assertThat(result.firstName()).isEqualTo("Ivan");
        assertThat(result.lastName()).isEqualTo("Ivanov");
        assertThat(result.email()).isEqualTo("ivan@test.com");
    }

    @Test
    void should_throw404_when_userNotFound() {
        // Given
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> userService.getProfile(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    // ─── updateProfile ───────────────────────────────────────────────────────

    @Test
    void should_updateProfile_when_validRequest() {
        // Given
        UUID id = UUID.randomUUID();
        UserEntity user = UserEntity.builder()
                .id(id)
                .firstName("Ivan")
                .lastName("Ivanov")
                .email("ivan@test.com")
                .passwordHash("hash")
                .build();
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.save(any(UserEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateProfileRequest request = new UpdateProfileRequest(
                "Petr", "Petrov", "https://img.example.com/1.png", LocalDate.of(1990, 5, 15));

        // When
        UserDto result = userService.updateProfile(id, request);

        // Then
        assertThat(result.firstName()).isEqualTo("Petr");
        assertThat(result.lastName()).isEqualTo("Petrov");
        assertThat(result.avatarUrl()).isEqualTo("https://img.example.com/1.png");
        assertThat(result.dateOfBirth()).isEqualTo(LocalDate.of(1990, 5, 15));
        verify(userRepository).save(any(UserEntity.class));
    }

    @Test
    void should_notChangeEmail_when_profileUpdated() {
        // Given
        UUID id = UUID.randomUUID();
        UserEntity user = UserEntity.builder()
                .id(id)
                .firstName("Ivan")
                .lastName("Ivanov")
                .email("ivan@test.com")
                .passwordHash("hash")
                .build();
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.save(any(UserEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateProfileRequest request = new UpdateProfileRequest("Petr", "Petrov", null, null);

        // When
        UserDto result = userService.updateProfile(id, request);

        // Then — email stays unchanged
        assertThat(result.email()).isEqualTo("ivan@test.com");
    }

    @Test
    void should_throw404_when_updatingNonExistentUser() {
        // Given
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        UpdateProfileRequest request = new UpdateProfileRequest("Petr", "Petrov", null, null);

        // When / Then
        assertThatThrownBy(() -> userService.updateProfile(id, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(id.toString());
    }
}

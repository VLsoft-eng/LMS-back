package com.example.lms.security;

import com.example.lms.entity.UserEntity;
import com.example.lms.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    @Test
    void should_returnUserDetails_whenEmailExists() {
        // Given
        UserEntity user = UserEntity.builder()
                .id(UUID.randomUUID())
                .email("user@example.com")
                .passwordHash("$2a$12$hashed_password")
                .firstName("Ivan")
                .lastName("Ivanov")
                .build();
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        // When
        UserDetails details = userDetailsService.loadUserByUsername("user@example.com");

        // Then
        assertThat(details.getUsername()).isEqualTo("user@example.com");
        assertThat(details.getPassword()).isEqualTo("$2a$12$hashed_password");
    }

    @Test
    void should_throwUsernameNotFoundException_whenEmailNotFound() {
        // Given
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        // When
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("ghost@example.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("ghost@example.com");
    }
}

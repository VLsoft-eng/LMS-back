package com.example.lms.service;

import com.example.lms.dto.UpdateProfileRequest;
import com.example.lms.dto.UserDto;
import com.example.lms.entity.UserEntity;
import com.example.lms.exception.ResourceNotFoundException;
import com.example.lms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * TICKET-BE-06: Business logic for user profile operations.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserDto getProfile(UUID userId) {
        UserEntity user = findOrThrow(userId);
        return toDto(user);
    }

    @Transactional
    public UserDto updateProfile(UUID userId, UpdateProfileRequest request) {
        UserEntity user = findOrThrow(userId);

        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setAvatarUrl(request.avatarUrl());
        user.setDateOfBirth(request.dateOfBirth());

        return toDto(userRepository.save(user));
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private UserEntity findOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }

    static UserDto toDto(UserEntity user) {
        return new UserDto(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getAvatarUrl(),
                user.getDateOfBirth(),
                user.getCreatedAt()
        );
    }
}

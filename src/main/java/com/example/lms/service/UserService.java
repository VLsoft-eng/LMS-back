package com.example.lms.service;

import com.example.lms.dto.UpdateProfileRequest;
import com.example.lms.dto.UserDto;
import com.example.lms.entity.UserEntity;
import com.example.lms.exception.ResourceNotFoundException;
import com.example.lms.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * TICKET-BE-06: Business logic for user profile operations.
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final FileStorageServiceImpl fileStorageService;
    private final String appBaseUrl;

    public UserService(UserRepository userRepository,
                       FileStorageServiceImpl fileStorageService,
                       @Value("${app.base-url:http://localhost:8080}") String appBaseUrl) {
        this.userRepository = userRepository;
        this.fileStorageService = fileStorageService;
        this.appBaseUrl = appBaseUrl;
    }

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
        if (request.avatarUrl() != null) {
            user.setAvatarUrl(request.avatarUrl());
        }
        user.setDateOfBirth(request.dateOfBirth());

        return toDto(userRepository.save(user));
    }

    @Transactional
    public UserDto uploadAvatar(UUID userId, MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Файл не должен быть пустым");
        }

        UserEntity user = findOrThrow(userId);
        String originalName = file.getOriginalFilename();
        String extension = extractImageExtension(file.getContentType(), originalName);
        String filename = fileStorageService.storeWithExtension(file, extension);
        user.setAvatarUrl(appBaseUrl + "/api/v1/files/" + filename);
        return toDto(userRepository.save(user));
    }

    private String extractImageExtension(String contentType, String originalName) {
        if (contentType != null) {
            if (contentType.contains("jpeg") || contentType.contains("jpg")) return ".jpg";
            if (contentType.contains("png")) return ".png";
            if (contentType.contains("gif")) return ".gif";
            if (contentType.contains("webp")) return ".webp";
            if (contentType.contains("heic")) return ".heic";
        }
        if (originalName != null && originalName.contains(".")) {
            return originalName.substring(originalName.lastIndexOf('.'));
        }
        return ".jpg";
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

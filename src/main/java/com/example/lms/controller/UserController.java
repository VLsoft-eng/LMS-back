package com.example.lms.controller;

import com.example.lms.dto.UpdateProfileRequest;
import com.example.lms.dto.UserDto;
import com.example.lms.entity.UserEntity;
import com.example.lms.security.CurrentUser;
import com.example.lms.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * TICKET-BE-06: REST endpoints for the authenticated user's own profile.
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * GET /api/v1/users/me
     * Returns the profile of the currently authenticated user.
     */
    @GetMapping("/me")
    public ResponseEntity<UserDto> getMe(@CurrentUser UserEntity currentUser) {
        return ResponseEntity.ok(userService.getProfile(currentUser.getId()));
    }

    /**
     * PUT /api/v1/users/me
     * Updates mutable profile fields. Email is read-only and cannot be changed here.
     */
    @PutMapping("/me")
    public ResponseEntity<UserDto> updateMe(@CurrentUser UserEntity currentUser,
                                            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateProfile(currentUser.getId(), request));
    }
}

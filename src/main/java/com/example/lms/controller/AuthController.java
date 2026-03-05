package com.example.lms.controller;

import com.example.lms.dto.AuthResponse;
import com.example.lms.dto.LoginRequest;
import com.example.lms.dto.RegisterRequest;
import com.example.lms.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * TICKET-BE-05: POST /api/v1/auth/register and POST /api/v1/auth/login.
 * No business logic — delegates entirely to AuthService.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}

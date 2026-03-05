package com.example.lms.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record RegisterRequest(

        @NotBlank
        String firstName,

        @NotBlank
        String lastName,

        @NotBlank
        @Email
        String email,

        @NotBlank
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password,

        LocalDate dateOfBirth,

        String avatarBase64
) {}

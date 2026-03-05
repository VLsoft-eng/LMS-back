package com.example.lms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AssignRoleRequest(

        @NotBlank(message = "Role is required")
        @Pattern(regexp = "TEACHER|STUDENT", message = "Role must be TEACHER or STUDENT")
        String role
) {}

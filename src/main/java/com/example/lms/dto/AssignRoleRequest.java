package com.example.lms.dto;

import com.example.lms.entity.Role;
import jakarta.validation.constraints.NotNull;

public record AssignRoleRequest(
        @NotNull(message = "Role is required")
        Role role
) {}

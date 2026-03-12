package com.example.lms.dto;

import com.example.lms.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Запрос на назначение роли участнику")
public record AssignRoleRequest(

        @Schema(description = "Новая роль. Допустимые значения: TEACHER, STUDENT (назначить OWNER нельзя).", example = "TEACHER",
                allowableValues = {"TEACHER", "STUDENT"})
        @NotNull(message = "Role is required")
        Role role
) {}

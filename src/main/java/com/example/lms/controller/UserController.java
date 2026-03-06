package com.example.lms.controller;

import com.example.lms.dto.UpdateProfileRequest;
import com.example.lms.dto.UserDto;
import com.example.lms.entity.UserEntity;
import com.example.lms.security.CurrentUser;
import com.example.lms.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Пользователь", description = "Профиль текущего пользователя")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "Получить профиль", description = "Возвращает профиль текущего аутентифицированного пользователя")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "401", description = "Требуется аутентификация")
    })
    @GetMapping("/me")
    public ResponseEntity<UserDto> getMe(@CurrentUser UserEntity currentUser) {
        return ResponseEntity.ok(userService.getProfile(currentUser.getId()));
    }

    @Operation(summary = "Обновить профиль", description = "Обновляет поля профиля. Email неизменяем.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Профиль обновлён"),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации"),
            @ApiResponse(responseCode = "401", description = "Требуется аутентификация")
    })
    @PutMapping("/me")
    public ResponseEntity<UserDto> updateMe(@CurrentUser UserEntity currentUser,
                                            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateProfile(currentUser.getId(), request));
    }
}

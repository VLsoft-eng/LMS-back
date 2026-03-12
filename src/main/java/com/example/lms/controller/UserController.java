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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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

    @Operation(summary = "Загрузить аватарку", description = "Загружает изображение как аватарку пользователя")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Аватарка обновлена"),
            @ApiResponse(responseCode = "400", description = "Некорректный файл"),
            @ApiResponse(responseCode = "401", description = "Требуется аутентификация")
    })
    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserDto> uploadAvatar(@CurrentUser UserEntity currentUser,
                                                @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(userService.uploadAvatar(currentUser.getId(), file));
    }
}

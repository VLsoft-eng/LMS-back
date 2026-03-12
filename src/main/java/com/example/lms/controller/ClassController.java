package com.example.lms.controller;

import com.example.lms.dto.ClassDto;
import com.example.lms.dto.CreateClassRequest;
import com.example.lms.dto.JoinClassRequest;
import com.example.lms.dto.UpdateClassRequest;
import com.example.lms.entity.UserEntity;
import com.example.lms.security.CurrentUser;
import com.example.lms.service.ClassService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Tag(name = "Классы", description = "CRUD для классов, вступление по коду")
@RestController
@RequestMapping("/api/v1/classes")
@RequiredArgsConstructor
public class ClassController {

    private final ClassService classService;

    @Operation(summary = "Создать класс", description = "Создаёт новый класс. Генерирует 8-символьный код. Создатель становится OWNER.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Класс создан"),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации"),
            @ApiResponse(responseCode = "401", description = "Требуется аутентификация")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClassDto createClass(@Valid @RequestBody CreateClassRequest request,
                                @CurrentUser UserEntity currentUser) {
        return classService.createClass(request, currentUser);
    }

    @Operation(summary = "Список моих классов", description = "Возвращает классы, в которых пользователь является участником")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "401", description = "Требуется аутентификация")
    })
    @GetMapping
    public Page<ClassDto> getMyClasses(@CurrentUser UserEntity currentUser,
                                       @PageableDefault(size = 20, sort = "createdAt")
                                       @Parameter(description = "Пагинация: page (с 0), size, sort (поле,asc|desc)", example = "page=0&size=20")
                                       Pageable pageable) {
        return classService.getMyClasses(currentUser.getId(), pageable);
    }

    @Operation(summary = "Получить класс по ID", description = "Детали класса. Доступно только участникам.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "401", description = "Требуется аутентификация"),
            @ApiResponse(responseCode = "403", description = "Нет доступа к классу"),
            @ApiResponse(responseCode = "404", description = "Класс не найден")
    })
    @GetMapping("/{id}")
    public ClassDto getClass(@PathVariable UUID id,
                             @CurrentUser UserEntity currentUser) {
        return classService.getClass(id, currentUser.getId());
    }

    @Operation(summary = "Вступить в класс", description = "Вступление в класс по реферальному коду")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Успешно вступил"),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации"),
            @ApiResponse(responseCode = "401", description = "Требуется аутентификация"),
            @ApiResponse(responseCode = "404", description = "Класс с таким кодом не найден")
    })
    @PostMapping("/join")
    public ClassDto joinClass(@Valid @RequestBody JoinClassRequest request,
                              @CurrentUser UserEntity currentUser) {
        return classService.joinClass(request.code(), currentUser);
    }

    @Operation(summary = "Обновить класс", description = "Переименование класса. Только OWNER.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Класс обновлён"),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации"),
            @ApiResponse(responseCode = "401", description = "Требуется аутентификация"),
            @ApiResponse(responseCode = "403", description = "Только OWNER может обновить класс"),
            @ApiResponse(responseCode = "404", description = "Класс не найден")
    })
    @PutMapping("/{id}")
    public ClassDto updateClass(@PathVariable UUID id,
                                @Valid @RequestBody UpdateClassRequest request,
                                @CurrentUser UserEntity currentUser) {
        return classService.updateClass(id, request, currentUser.getId());
    }

    @Operation(summary = "Удалить класс", description = "Удаление класса. Только OWNER.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Класс удалён"),
            @ApiResponse(responseCode = "401", description = "Требуется аутентификация"),
            @ApiResponse(responseCode = "403", description = "Только OWNER может удалить класс"),
            @ApiResponse(responseCode = "404", description = "Класс не найден")
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteClass(@PathVariable UUID id,
                            @CurrentUser UserEntity currentUser) {
        classService.deleteClass(id, currentUser.getId());
    }

    @Operation(summary = "Получить код класса", description = "Реферальный код для вступления. OWNER и TEACHER.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "401", description = "Требуется аутентификация"),
            @ApiResponse(responseCode = "403", description = "Только OWNER/TEACHER"),
            @ApiResponse(responseCode = "404", description = "Класс не найден")
    })
    @GetMapping("/{id}/code")
    public Map<String, String> getClassCode(@PathVariable UUID id,
                                            @CurrentUser UserEntity currentUser) {
        return Map.of("code", classService.getClassCode(id, currentUser.getId()));
    }

    @Operation(summary = "Обновить код класса", description = "Генерирует новый код приглашения. OWNER и TEACHER.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Новый код сгенерирован"),
            @ApiResponse(responseCode = "401", description = "Требуется аутентификация"),
            @ApiResponse(responseCode = "403", description = "Только OWNER/TEACHER"),
            @ApiResponse(responseCode = "404", description = "Класс не найден")
    })
    @PostMapping("/{id}/code/regenerate")
    public ClassDto regenerateCode(@PathVariable UUID id,
                                   @CurrentUser UserEntity currentUser) {
        return classService.regenerateCode(id, currentUser.getId());
    }
}

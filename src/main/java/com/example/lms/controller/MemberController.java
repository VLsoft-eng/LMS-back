package com.example.lms.controller;

import com.example.lms.dto.AssignRoleRequest;
import com.example.lms.dto.MemberDto;
import com.example.lms.entity.UserEntity;
import com.example.lms.security.CurrentUser;
import com.example.lms.service.MemberService;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Участники класса", description = "Список участников и управление ролями")
@RestController
@RequestMapping("/api/v1/classes/{classId}/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @Operation(summary = "Список участников", description = "Возвращает участников класса с пагинацией")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "401", description = "Требуется аутентификация"),
            @ApiResponse(responseCode = "403", description = "Нет доступа к классу"),
            @ApiResponse(responseCode = "404", description = "Класс не найден")
    })
    @GetMapping
    public Page<MemberDto> getMembers(@PathVariable UUID classId,
                                      @CurrentUser UserEntity currentUser,
                                      @PageableDefault(size = 50, sort = "role")
                                      @Parameter(description = "Пагинация: page (с 0), size, sort (поле,asc|desc)", example = "page=0&size=50")
                                      Pageable pageable) {
        return memberService.getMembers(classId, currentUser.getId(), pageable);
    }

    @Operation(summary = "Назначить роль", description = "Изменение роли участника. Только OWNER. Нельзя изменить OWNER, нельзя назначить OWNER.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Роль обновлена"),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации"),
            @ApiResponse(responseCode = "401", description = "Требуется аутентификация"),
            @ApiResponse(responseCode = "403", description = "Только OWNER может управлять ролями"),
            @ApiResponse(responseCode = "404", description = "Класс или участник не найден")
    })
    @PutMapping("/{memberUserId}/role")
    public MemberDto assignRole(@PathVariable UUID classId,
                                @PathVariable UUID memberUserId,
                                @Valid @RequestBody AssignRoleRequest request,
                                @CurrentUser UserEntity currentUser) {
        return memberService.assignRole(classId, memberUserId, request, currentUser.getId());
    }

    @Operation(summary = "Удалить участника", description = "Удаляет участника из класса. Только OWNER. Нельзя удалить OWNER.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Участник удалён"),
            @ApiResponse(responseCode = "401", description = "Требуется аутентификация"),
            @ApiResponse(responseCode = "403", description = "Только OWNER может удалять участников"),
            @ApiResponse(responseCode = "404", description = "Класс или участник не найден")
    })
    @DeleteMapping("/{memberUserId}")
    public ResponseEntity<Void> removeMember(@PathVariable UUID classId,
                                             @PathVariable UUID memberUserId,
                                             @CurrentUser UserEntity currentUser) {
        memberService.removeMember(classId, memberUserId, currentUser.getId());
        return ResponseEntity.noContent().build();
    }
}

package com.example.lms.controller;

import com.example.lms.dto.*;
import com.example.lms.entity.UserEntity;
import com.example.lms.security.CurrentUser;
import com.example.lms.service.ShuffleService;
import com.example.lms.service.TeamService;
import io.swagger.v3.oas.annotations.Operation;
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

import java.util.List;
import java.util.UUID;

/**
 * TICKET-BE-28: REST-эндпоинты команд.
 */
@Tag(name = "Команды", description = "Управление командами класса")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;
    private final ShuffleService shuffleService;

    @Operation(summary = "Создать команду")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Команда создана"),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации"),
            @ApiResponse(responseCode = "403", description = "Только OWNER/TEACHER"),
            @ApiResponse(responseCode = "404", description = "Класс или пользователь не найден")
    })
    @PostMapping("/classes/{classId}/teams")
    @ResponseStatus(HttpStatus.CREATED)
    public TeamDto createTeam(@PathVariable UUID classId,
                              @Valid @RequestBody CreateTeamRequest request,
                              @CurrentUser UserEntity currentUser) {
        return teamService.createTeam(classId, request, currentUser);
    }

    @Operation(summary = "Автоматическая разбивка на команды")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Команды созданы"),
            @ApiResponse(responseCode = "400", description = "Некорректные параметры"),
            @ApiResponse(responseCode = "403", description = "Только OWNER/TEACHER"),
            @ApiResponse(responseCode = "409", description = "Постоянные команды уже существуют")
    })
    @PostMapping("/classes/{classId}/teams/shuffle")
    @ResponseStatus(HttpStatus.CREATED)
    public ShuffleResponse shuffleTeams(@PathVariable UUID classId,
                                        @Valid @RequestBody ShuffleRequest request,
                                        @CurrentUser UserEntity currentUser) {
        return shuffleService.shuffle(classId, request, currentUser);
    }

    @Operation(summary = "Список команд класса")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "403", description = "Не участник класса")
    })
    @GetMapping("/classes/{classId}/teams")
    public Page<TeamListItemDto> getTeams(@PathVariable UUID classId,
                                          @RequestParam(required = false) UUID assignmentId,
                                          @CurrentUser UserEntity currentUser,
                                          @PageableDefault(size = 20) Pageable pageable) {
        return teamService.getTeams(classId, assignmentId, currentUser.getId(), pageable);
    }

    @Operation(summary = "Детали команды")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "403", description = "Не участник класса"),
            @ApiResponse(responseCode = "404", description = "Команда не найдена")
    })
    @GetMapping("/classes/{classId}/teams/{teamId}")
    public TeamDto getTeam(@PathVariable UUID classId,
                           @PathVariable UUID teamId,
                           @CurrentUser UserEntity currentUser) {
        return teamService.getTeam(classId, teamId, currentUser.getId());
    }

    @Operation(summary = "Обновить команду")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "403", description = "Только OWNER/TEACHER"),
            @ApiResponse(responseCode = "404", description = "Команда не найдена")
    })
    @PutMapping("/classes/{classId}/teams/{teamId}")
    public TeamDto updateTeam(@PathVariable UUID classId,
                              @PathVariable UUID teamId,
                              @Valid @RequestBody UpdateTeamRequest request,
                              @CurrentUser UserEntity currentUser) {
        return teamService.updateTeam(classId, teamId, request, currentUser.getId());
    }

    @Operation(summary = "Удалить команду")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Удалено"),
            @ApiResponse(responseCode = "403", description = "Только OWNER/TEACHER"),
            @ApiResponse(responseCode = "404", description = "Команда не найдена")
    })
    @DeleteMapping("/classes/{classId}/teams/{teamId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTeam(@PathVariable UUID classId,
                           @PathVariable UUID teamId,
                           @CurrentUser UserEntity currentUser) {
        teamService.deleteTeam(classId, teamId, currentUser.getId());
    }

    @Operation(summary = "Мои команды (для студента)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Успешно"),
            @ApiResponse(responseCode = "403", description = "Не участник класса")
    })
    @GetMapping("/classes/{classId}/teams/my")
    public List<TeamDto> getMyTeams(@PathVariable UUID classId,
                                    @CurrentUser UserEntity currentUser) {
        return teamService.getMyTeams(classId, currentUser.getId());
    }

    @Operation(summary = "Добавить участника в команду")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Участник добавлен"),
            @ApiResponse(responseCode = "403", description = "Только OWNER/TEACHER"),
            @ApiResponse(responseCode = "404", description = "Команда или пользователь не найден"),
            @ApiResponse(responseCode = "409", description = "Уже в команде")
    })
    @PostMapping("/classes/{classId}/teams/{teamId}/members")
    @ResponseStatus(HttpStatus.CREATED)
    public TeamDto addMember(@PathVariable UUID classId,
                             @PathVariable UUID teamId,
                             @Valid @RequestBody AddTeamMemberRequest request,
                             @CurrentUser UserEntity currentUser) {
        return teamService.addMember(classId, teamId, request, currentUser.getId());
    }

    @Operation(summary = "Удалить участника из команды")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Участник удалён"),
            @ApiResponse(responseCode = "403", description = "Только OWNER/TEACHER"),
            @ApiResponse(responseCode = "404", description = "Участник не найден")
    })
    @DeleteMapping("/classes/{classId}/teams/{teamId}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(@PathVariable UUID classId,
                             @PathVariable UUID teamId,
                             @PathVariable UUID userId,
                             @CurrentUser UserEntity currentUser) {
        teamService.removeMember(classId, teamId, userId, currentUser.getId());
    }
}

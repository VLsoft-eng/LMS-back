package com.example.lms.controller;

import com.example.lms.dto.ClassStatsDto;
import com.example.lms.entity.UserEntity;
import com.example.lms.security.CurrentUser;
import com.example.lms.service.StatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Статистика", description = "Успеваемость студентов в классе. Только для OWNER и TEACHER.")
@RestController
@RequestMapping("/api/v1/classes/{classId}/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @Operation(
            summary = "Статистика класса",
            description = """
                    Возвращает статистику успеваемости всех студентов класса.
                    Доступно только **OWNER** и **TEACHER**.
                    
                    Включает:
                    - Общую информацию по классу (кол-во заданий, студентов, средняя оценка, процент сдавших)
                    - По каждому студенту: кол-во сданных/оценённых работ, среднюю оценку, кол-во пропущенных дедлайнов
                    - Студенты отсортированы по средней оценке (убывание)
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Статистика получена"),
            @ApiResponse(responseCode = "401", description = "Требуется аутентификация"),
            @ApiResponse(responseCode = "403", description = "Только OWNER/TEACHER"),
            @ApiResponse(responseCode = "404", description = "Класс не найден")
    })
    @GetMapping
    public ClassStatsDto getClassStats(@PathVariable UUID classId,
                                       @CurrentUser UserEntity currentUser) {
        return statsService.getClassStats(classId, currentUser.getId());
    }
}

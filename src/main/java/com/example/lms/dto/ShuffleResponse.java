package com.example.lms.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Результат автоматической разбивки на команды")
public record ShuffleResponse(
        @Schema(description = "Созданные команды")
        List<TeamDto> teams,

        @Schema(description = "Всего студентов")
        int totalStudents,

        @Schema(description = "Студентов на команду (среднее)")
        int studentsPerTeam
) {}

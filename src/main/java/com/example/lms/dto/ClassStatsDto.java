package com.example.lms.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "Статистика класса")
public record ClassStatsDto(

        @Schema(description = "ID класса")
        UUID classId,

        @Schema(description = "Название класса", example = "Математика 10А")
        String className,

        @Schema(description = "Всего заданий в классе", example = "10")
        int totalAssignments,

        @Schema(description = "Количество студентов", example = "20")
        int totalStudents,

        @Schema(description = "Средняя оценка по классу (null если нет оценённых работ)", example = "74.3", nullable = true)
        Double classAverageGrade,

        @Schema(description = "Процент сдавших хотя бы одно задание", example = "85.0")
        double submissionRate,

        @Schema(description = "Статистика по каждому студенту")
        List<StudentStatDto> students
) {}

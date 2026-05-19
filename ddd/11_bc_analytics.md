# BC-7: Analytics & Reporting

## Purpose

Агрегированная статистика класса для преподавателей: успеваемость студентов, покрытие заданиями.

## Strategic Classification

Supporting Subdomain. Читает данные из BC Assignment + BC Grading.

## Inbound Communication

| Запрос | Описание |
|--------|----------|
| `GetClassStats(classId)` | Статистика: число назначений, студентов, средний балл, детализация по студентам |

## Ubiquitous Language

| Термин | Определение |
|--------|------------|
| Stats | Агрегированные показатели по классу |
| Average Grade | Средний нормализованный балл по классу (0-100) |
| Submission Rate | Доля сданных работ от ожидаемых |
| Missed Deadlines | Число просроченных сдач по студенту |

## Business Decisions

1. Доступ только для OWNER/TEACHER.

## Where in codebase

| Слой | Расположение |
|------|-------------|
| Бэк | `controller/StatsController`, `dto/ClassStatsDto` |
| Веб | `src/features/classes/` (страница статистики класса) |
| iOS | `Views/Classes/ClassStatsView.swift` |

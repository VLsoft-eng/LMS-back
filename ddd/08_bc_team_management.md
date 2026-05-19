# BC-4: Team Management

## Purpose

Формирование студенческих команд (вручную или автоматически), управление составом.

## Strategic Classification

Supporting Subdomain. Тип взаимодействия: Upstream для BC Grading (предоставляет `teamId`).

## Inbound Communication

| Команда / Запрос | Источник | Описание |
|-----------------|---------|----------|
| `CreateTeam(classId, name)` | OWNER/TEACHER | Создать команду вручную |
| `ShuffleTeams(classId, assignmentId, teamSize)` | OWNER/TEACHER | Авто-распределение по командам |
| `AddTeamMember(teamId, userId)` | OWNER/TEACHER | Добавить участника |
| `RemoveTeamMember(teamId, userId)` | OWNER/TEACHER | Удалить участника |
| `GetMyTeams(classId)` | STUDENT | Список своих команд |

## Outbound Communication

| Зависимость | Направление |
|------------|------------|
| `teamId` | BC Grading (при создании TeamGrade) |

## Ubiquitous Language

| Термин | Определение |
|--------|------------|
| Team | Группа студентов, работающих совместно |
| Team Leader | Выделенный участник команды (флаг `isLeader`) |
| Shuffle | Алгоритм авто-разбивки класса на команды |
| Team Assignment Scope | Привязка команды к конкретному заданию |

## Business Decisions

1. Студент не может состоять дважды в одной команде.
2. Shuffle создаёт команды равного размера (алгоритм в `TeamService`).

## Where in codebase

| Слой | Расположение |
|------|-------------|
| Бэк | `controller/TeamController`, `entity/TeamEntity`, `entity/TeamMemberEntity` |
| Веб | `src/features/teams/`, роуты `/classes/:id/teams` |
| iOS | `Views/Teams/` |

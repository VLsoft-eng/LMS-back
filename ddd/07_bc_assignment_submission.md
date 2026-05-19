# BC-3: Assignment & Submission

## Purpose

Публикация учебных заданий (с дедлайнами, файлами, типами), приём студенческих работ, дискуссии в рамках задания.

## Strategic Classification

Core Subdomain. Тип взаимодействия: Upstream для BC Grading (предоставляет `assignmentId`, `submissionId`).

## Inbound Communication

| Команда / Запрос | Источник | Описание |
|-----------------|---------|----------|
| `CreateAssignment(classId, title, description, deadline, isTeamBased, files)` | OWNER/TEACHER | Создать задание |
| `CreateQuickAssignment(classId, title)` | OWNER/TEACHER | Быстрое аудиторное задание |
| `SubmitWork(assignmentId, answerText, files)` | STUDENT | Сдать / перезаписать работу |
| `CancelSubmission(assignmentId)` | STUDENT | Отозвать сдачу (до дедлайна, если не оценена) |
| `AddComment(assignmentId, text)` | Все роли | Написать комментарий к заданию |
| `GetMySubmission(assignmentId)` | STUDENT | Просмотр своей работы |

## Outbound Communication

| Зависимость | Направление |
|------------|------------|
| Файлы задания / работы | File Storage BC |
| `submissionId` | BC Grading (при создании Assessment) |

## Ubiquitous Language

| Термин | Определение |
|--------|------------|
| Assignment | Учебная задача, опубликованная преподавателем |
| Deadline | Крайний срок сдачи работы |
| Submission | Материалы студента в ответ на задание |
| STANDARD Assignment | Обычное задание с дедлайном |
| QUICK Assignment | Аудиторная задача без дедлайн-трекинга |
| Team-based | Флаг: работа сдаётся от команды, а не индивидуально |
| Comment | Сообщение любого участника к заданию |
| File Attachment | Файл, прикреплённый к заданию или работе |

## Business Decisions

1. Студент может иметь только одну активную сдачу на задание (upsert-семантика).
2. Отзыв сдачи невозможен после дедлайна или после выставления оценки.
3. Задание хранит `rubricId` как слабую ссылку на BC Grading.
4. `isTeamBased = true` означает: оценка выставляется через `TeamGrade`, а не через `Submission.grade`.

## Where in codebase

| Слой | Расположение |
|------|-------------|
| Бэк | `controller/AssignmentController`, `controller/SubmissionController`, `controller/CommentController`, `controller/QuickAssignmentController`, `entity/AssignmentEntity`, `entity/SubmissionEntity`, `entity/CommentEntity` |
| Веб | `src/features/assignments/`, `src/features/submissions/`, `src/features/comments/` |
| iOS | `Views/Assignments/`, `Views/Comments/` |

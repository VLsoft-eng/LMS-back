# Discovery Log

## Бэкенд (локально, `com.example.lms`)

| Категория | Что прочитано |
|-----------|--------------|
| Конфиги | `build.gradle.kts` — Spring Boot 4.0.3, Java 21, PostgreSQL, Flyway, JJWT, SpringDoc OpenAPI |
| Сущности | `UserEntity`, `ClassEntity`, `ClassMemberEntity`, `AssignmentEntity`, `SubmissionEntity`, `CommentEntity`, `TeamEntity`, `TeamMemberEntity`, `TeamGradeEntity`, `IndividualGradeAdjustmentEntity`, `RubricTemplateEntity`, `CriterionTemplateEntity`, `RubricEntity`, `CriterionEntity`, `AssessmentEntity`, `CriterionScoreEntity` |
| Контроллеры | `AuthController`, `UserController`, `ClassController`, `MemberController`, `AssignmentController`, `SubmissionController`, `CommentController`, `TeamController`, `TeamGradeController`, `RubricTemplateController`, `RubricController`, `AssessmentController`, `FileController`, `StatsController`, `QuickAssignmentController` — итого 62 endpoint |
| Миграции | V1-V11 (Flyway): V9 — rubric_templates/criterion_templates, V10 — rubrics/criteria (снимки), V11 — assessments/criterion_scores |
| Перечисления | `Role` (OWNER/TEACHER/STUDENT), `CriterionKind` (BOOLEAN/PERCENT/SCORE), `CriterionRole` (PRIMARY/BONUS) |
| Сервисный слой | Отдельные `*Service` для каждого агрегата; `RubricScoreCalculator` (расчётная логика) |

## Веб-фронт — [github.com/Sunrize1/LMS](https://github.com/Sunrize1/LMS)

| Категория | Что найдено |
|-----------|------------|
| Стек | React 19, TypeScript, Vite 7, React Router 6, TanStack Query, Zustand, Tailwind CSS, Zod |
| Структура | `src/features/{assignments,auth,classes,comments,submissions,teams,users,rubrics}` |
| Роуты | `/classes`, `/classes/:id`, `/classes/:id/settings`, `/classes/:id/teams`, `/classes/:id/members`, `/classes/:id/assignments/:id`, `/submissions/:id`, `/classes/:id/rubric-templates`, `/me/grades`, `/profile`, `/join` |
| Rubric-компоненты | `AssessmentForm.tsx`, `AssessmentView.tsx`, `AttachRubricModal.tsx`, `RubricTemplateEditor.tsx`, `RubricViewer.tsx`, `TeamRubricWidget.tsx` |
| Domain-логика | `domain/calculator.ts` (формула расчёта баллов), `domain/decimal.ts` (безопасная арифметика) |

## Мобильный фронт (iOS) — [github.com/BogdanTarchenko/LMS](https://github.com/BogdanTarchenko/LMS)

| Категория | Что найдено |
|-----------|------------|
| Стек | Swift, SwiftUI, MVVM, Custom APIService |
| Навигация | `MainTabView`: 2 таба (Классы, Профиль), NavigationStack с `navigationDestination` |
| Rubric-экраны | `RubricTemplateListView`, `RubricTemplateEditorView`, `AttachRubricSheet`, `RubricAssessmentView`, `AssessmentResultView`, `MyAssessmentView`, `CriterionEditorSheet` — 7 экранов |
| Модели | `CriterionKind`, `CriterionRole`, `CriterionTemplate`, `RubricTemplate`, `Criterion`, `Rubric`, `Assessment`, `CriterionScore`, `MyAssessment`, `RubricExportPayload` |
| Сервисы | `RubricScoreCalculator.swift` — логика расчёта на клиенте |

## Источник требований

ТЗ «Система критериев оценивания» предоставлено стейкхолдером напрямую. Внешних систем трекинга (Linear, Jira, Confluence) не обнаружено.

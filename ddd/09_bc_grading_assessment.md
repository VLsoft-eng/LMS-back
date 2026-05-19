# BC-5: Grading & Assessment (Core Domain)

## Purpose

Оценивание студенческих работ: от простого балла до многокритериального рубрикованного расчёта с бонусными мультипликаторами. Управление шаблонами рубрик, привязка рубрики к заданию, расчёт итогового балла.

## Strategic Classification

Core Subdomain. Наибольшая бизнес-ценность. Наиболее сложная логика.

## Inbound Communication

| Команда / Запрос | Источник | Описание |
|-----------------|---------|----------|
| `CreateRubricTemplate(classId, name, totalMaxPoints, criteria[])` | OWNER/TEACHER | Создать переиспользуемый шаблон |
| `UpdateRubricTemplate(id, ...)` | OWNER/TEACHER | Полная замена шаблона |
| `ExportTemplate(id)` | OWNER/TEACHER | Экспорт в JSON для переноса между классами |
| `ImportTemplate(classId, file/json)` | OWNER/TEACHER | Импорт шаблона |
| `AttachRubric(assignmentId, templateId / ad-hoc criteria[])` | OWNER/TEACHER | Зафиксировать рубрику для задания (снимок) |
| `DetachRubric(assignmentId)` | OWNER/TEACHER | Открепить рубрику (запрещено, если есть оценки) |
| `CreateAssessment(assignmentId, submissionId? / teamGradeId?, scores[])` | OWNER/TEACHER | Выставить оценку по критериям |
| `UpdateAssessment(assessmentId, scores[])` | OWNER/TEACHER | Пересчитать оценку |
| `DeleteAssessment(assessmentId)` | OWNER/TEACHER | Удалить оценку |
| `GradeSubmission(submissionId, grade 0-100)` | OWNER/TEACHER | Простая оценка без рубрики |
| `CreateTeamGrade(teamId, assignmentId, grade)` | OWNER/TEACHER | Командная оценка |
| `SetIndividualAdjustment(teamGradeId, studentId, adjustment)` | OWNER/TEACHER | Индивидуальная поправка -50..+50 |
| `GetMyAssessments` | STUDENT | Просмотр своих оценок |
| `GetClassStats(classId)` | OWNER/TEACHER | Агрегированная статистика |

## Outbound Communication

| Зависимость | Направление |
|------------|------------|
| `submissionId`, `assignmentId` | Принимает из BC Assignment |
| `teamId`, `teamGradeId` | Принимает из BC Team |
| JSON-файл при экспорте/импорте | File Storage BC |

## Ubiquitous Language

| Термин | Определение |
|--------|------------|
| RubricTemplate | Переиспользуемая конфигурация критериев на уровне класса |
| CriterionTemplate | Один критерий в шаблоне: тип, роль, веса |
| Rubric | Иммутабельный снимок шаблона, прикреплённый к заданию |
| Criterion | Снимок критерия в рубрике задания |
| CriterionKind | Тип критерия: BOOLEAN (да/нет), PERCENT (0-100%), SCORE (числовой диапазон) |
| CriterionRole | PRIMARY вносит баллы; BONUS задаёт мультипликатор |
| maxPoints | Максимум баллов по PRIMARY-критерию (часть `totalMaxPoints` рубрики) |
| maxCoefficient | Максимальный мультипликатор BONUS-критерия (1.0001-2.0000) |
| Assessment | Результат оценивания работы или команды по всем критериям |
| CriterionScore | Конкретное значение по одному критерию в рамках оценки |
| primarySum | Сумма баллов по всем PRIMARY-критериям до применения бонуса |
| bonusMultiplier | Итоговый мультипликатор от BONUS-критериев (произведение) |
| finalScore | `primarySum x bonusMultiplier` (может превышать `totalMaxPoints` при `allowOvercap = true`) |
| finalScoreNormalized | `finalScore / totalMaxPoints x 100`, округлено до целых, 0-100 |
| allowOvercap | Флаг: разрешить итогу превысить `totalMaxPoints` |
| IndividualAdjustment | Сдвиг -50..+50 к командной оценке для конкретного студента |
| Grade | Простой балл 0-100 на Submission без рубрики |
| TeamGrade | Командный балл 0-100 |

## Business Decisions

1. **Снимок иммутабелен.** Rubric создаётся через фабричный метод `snapshotFrom(template)` и не изменяется после `frozenAt`. Изменение шаблона не влияет ретроактивно на уже оцененные работы.
2. **Ровно одна рубрика на задание.** Rubric -> Assignment — взаимно-однозначное отношение (`UNIQUE assignment_id`).
3. **XOR-инвариант Assessment.** Каждая оценка привязана ровно к одному из (`submissionId`, `teamGradeId`), но не к обоим и не ни к одному.
4. **PRIMARY sum = totalMaxPoints.** Сумма `maxPoints` всех PRIMARY-критериев должна равняться `totalMaxPoints` рубрики. Проверяется в `RubricTemplateEntity.validateInvariants()`.
5. **Удаление шаблона запрещено**, если от него созданы Rubric-снимки.
6. **Удаление рубрики запрещено**, если существуют Assessment-ы.
7. **BONUS-критерий** требует `maxCoefficient` в диапазоне (1.0000, 2.0000], не имеет `maxPoints`.

## Where in codebase

| Слой | Расположение |
|------|-------------|
| Бэк — контроллеры | `controller/RubricTemplateController`, `controller/RubricController`, `controller/AssessmentController`, `controller/TeamGradeController` |
| Бэк — сущности | `entity/RubricTemplateEntity`, `entity/CriterionTemplateEntity`, `entity/RubricEntity`, `entity/CriterionEntity`, `entity/AssessmentEntity`, `entity/CriterionScoreEntity`, `entity/TeamGradeEntity`, `entity/IndividualGradeAdjustmentEntity` |
| Бэк — перечисления | `entity/CriterionKind`, `entity/CriterionRole` |
| Бэк — миграции | `V9__rubric_templates.sql`, `V10__rubrics_snapshots.sql`, `V11__assessments.sql` |
| Веб | `src/features/rubrics/` — `AssessmentForm`, `RubricTemplateEditor`, `AttachRubricModal`, `TeamRubricWidget`, `domain/calculator.ts` |
| iOS | `Views/Rubrics/` (7 экранов), `Models/Rubric.swift`, `ViewModels/AssessmentViewModel.swift`, `Services/RubricScoreCalculator.swift` |

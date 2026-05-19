# BC-2: Class & Enrollment Management

## Purpose

Создание учебных классов, генерация инвайт-кодов, управление составом и ролями участников.

## Strategic Classification

Supporting Subdomain. Тип взаимодействия: Upstream для BC Assignment, Team, Assessment (предоставляет `classId` и состав участников).

## Inbound Communication

| Команда / Запрос | Источник | Описание |
|-----------------|---------|----------|
| `CreateClass(name)` | OWNER | Создать класс, сгенерировать 8-символьный код |
| `JoinClass(code)` | STUDENT | Вступить в класс по коду |
| `RegenerateCode(classId)` | OWNER/TEACHER | Сбросить инвайт-код |
| `AssignRole(classId, userId, role)` | OWNER | Изменить роль участника |
| `RemoveMember(classId, userId)` | OWNER | Исключить участника |
| `GetClassMembers(classId)` | Все роли | Список участников с ролями |

## Outbound Communication

| Зависимость | Получатель |
|------------|-----------|
| `ClassMembership { classId, userId, role }` | BC Assignment, BC Team, BC Assessment — для проверки прав |

## Ubiquitous Language

| Термин | Определение |
|--------|------------|
| Class | Учебная группа, организованная вокруг набора заданий |
| Invite Code | 8-символьный уникальный код для вступления в класс |
| Member | Пользователь, состоящий в классе |
| OWNER | Создатель класса; единственный, кто управляет составом |
| TEACHER | Преподаватель с правом создания и оценивания заданий |
| STUDENT | Студент с правом сдачи работ и просмотра оценок |
| Enrollment | Факт вступления пользователя в класс |

## Business Decisions

1. Один пользователь — одна запись в классе (`UNIQUE` на `class_id + user_id`).
2. Инвайт-код генерируется случайно и уникален глобально.
3. Роль OWNER не может быть назначена через API — только при создании.

## Where in codebase

| Слой | Расположение |
|------|-------------|
| Бэк | `controller/ClassController`, `controller/MemberController`, `entity/ClassEntity`, `entity/ClassMemberEntity`, `entity/Role` |
| Веб | `src/features/classes/`, роуты `/classes`, `/classes/:id/settings`, `/classes/:id/members`, `/join` |
| iOS | `Views/Classes/ClassListView.swift`, `ClassDetailView.swift`, `ClassSettingsView.swift`, `JoinClassSheet.swift` |

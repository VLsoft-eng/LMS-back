# BC-1: Identity & Access (IAM)

## Purpose

Управление учётными записями пользователей: регистрация, аутентификация, хранение профиля.

## Strategic Classification

Generic Subdomain. Тип взаимодействия: Upstream (предоставляет идентификатор пользователя всем остальным BC).

## Inbound Communication

| Команда / Запрос | Источник | Описание |
|-----------------|---------|----------|
| `RegisterUser(email, password, firstName, lastName)` | Веб/iOS | Создать аккаунт |
| `Login(email, password)` | Веб/iOS | Получить JWT |
| `UpdateProfile(firstName, lastName, avatarUrl, dob)` | Веб/iOS | Обновить данные |
| `UploadAvatar(file)` | Веб/iOS | Сменить фото |

## Outbound Communication

| Событие / Зависимость | Получатель |
|----------------------|-----------|
| `UserIdentity { userId, email }` через JWT | Все BC |
| Файл аватара | File Storage BC |

## Ubiquitous Language

| Термин | Определение |
|--------|------------|
| User | Зарегистрированный участник платформы |
| Credentials | Пара email + пароль |
| JWT Token | Подписанный токен доступа, содержащий userId |
| Profile | Публичные данные пользователя (имя, аватар, дата рождения) |
| Avatar | Изображение профиля пользователя |

## Business Decisions

1. Email уникален в системе.
2. Пароль хранится только в виде хеша (`passwordHash`).
3. JWT не содержит роль в классе — роль определяется в BC Class Management при каждом запросе.

## Where in codebase

| Слой | Расположение |
|------|-------------|
| Бэк | `controller/AuthController`, `controller/UserController`, `service/AuthService`, `service/UserService`, `entity/UserEntity`, `security/JwtService` |
| Веб | `src/features/auth/`, `src/store/` (authStore, Zustand) |
| iOS | `Services/AuthManager.swift`, `Views/Auth/LoginView.swift`, `Views/Auth/RegisterView.swift` |

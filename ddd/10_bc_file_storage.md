# BC-6: File Storage

## Purpose

Хранение и выдача файловых вложений (задания, работы, аватары).

## Strategic Classification

Generic Subdomain. Stateless API-обёртка над файловым хранилищем.

## Inbound Communication

| Команда | Описание |
|---------|----------|
| `UploadFile(file)` | Сохранить файл, вернуть URL/путь |
| `DownloadFile(filename)` | Отдать файл (публично, без авторизации) |

## Ubiquitous Language

| Термин | Определение |
|--------|------------|
| File | Бинарный файл, загруженный пользователем |
| Filename | Уникальное имя файла в хранилище |
| File Path | Путь к файлу, хранится в `TEXT[]` полях Assignment и Submission |
| File URL | Публичный URL для скачивания |

## Business Decisions

1. Публичный доступ к файлам по URL без проверки токена. URL сам является секретом.

## Where in codebase

| Слой | Расположение |
|------|-------------|
| Бэк | `controller/FileController`, `service/FileStorageServiceImpl` |

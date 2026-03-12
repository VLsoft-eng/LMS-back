package com.example.lms.config;

import com.example.lms.entity.UserEntity;
import com.example.lms.security.CurrentUser;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    static {
        SpringDocUtils.getConfig()
                .addAnnotationsToIgnore(CurrentUser.class)
                .addRequestWrapperToIgnore(UserEntity.class);
    }

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("LMS API")
                        .version("1.0.0")
                        .description("""
                                ## Learning Management System API
                                
                                REST API для мобильного LMS-приложения.
                                
                                ### Аутентификация
                                Все эндпоинты (кроме `/auth/*` и `/files/*`) требуют JWT-токен.
                                Получите токен через `POST /api/v1/auth/login` или `POST /api/v1/auth/register`,
                                затем нажмите кнопку **Authorize** и введите токен.
                                
                                ### Роли
                                | Роль | Описание |
                                |------|----------|
                                | `OWNER` | Создатель класса. Полный доступ: управление участниками, ролями, заданиями. |
                                | `TEACHER` | Может создавать задания, оценивать ответы, обновлять код приглашения. |
                                | `STUDENT` | Может просматривать задания и сдавать ответы. |
                                
                                ### Коды ошибок
                                | Код | Значение |
                                |-----|----------|
                                | 400 | Ошибка валидации запроса |
                                | 401 | Требуется аутентификация |
                                | 403 | Недостаточно прав |
                                | 404 | Ресурс не найден |
                                | 409 | Конфликт (например, email уже занят) |
                                """)
                        .contact(new Contact()
                                .name("LMS Team")
                                .email("support@lms.example.com")))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT токен. Получить через `POST /api/v1/auth/login` или `POST /api/v1/auth/register`. Вставьте только сам токен, без префикса Bearer.")))
                .security(List.of(new SecurityRequirement().addList(SECURITY_SCHEME_NAME)))
                .tags(List.of(
                        new Tag().name("Аутентификация")
                                .description("Регистрация и вход. Эндпоинты **не требуют** JWT-токена."),
                        new Tag().name("Пользователь")
                                .description("Профиль текущего аутентифицированного пользователя: просмотр, редактирование, загрузка аватарки."),
                        new Tag().name("Классы")
                                .description("Создание и управление классами. Вступление по реферальному коду. Обновление кода приглашения."),
                        new Tag().name("Участники класса")
                                .description("Просмотр участников, назначение ролей (`TEACHER` / `STUDENT`), удаление участников."),
                        new Tag().name("Задания")
                                .description("Создание заданий с файлами, просмотр списка и деталей. OWNER/TEACHER создают, все участники просматривают."),
                        new Tag().name("Ответы")
                                .description("Сдача ответов студентами (текст + файлы), просмотр, отмена. Оценивание преподавателем (0–100)."),
                        new Tag().name("Комментарии")
                                .description("Комментарии к заданиям. Доступны всем участникам класса."),
                        new Tag().name("Файлы")
                                .description("Публичная раздача загруженных файлов (аватарки, вложения). **Не требует** JWT-токена."),
                        new Tag().name("Статистика")
                                .description("Успеваемость студентов в классе. Доступно только OWNER и TEACHER.")
                ));
    }
}

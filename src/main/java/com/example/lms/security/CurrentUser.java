package com.example.lms.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * TICKET-BE-06: Injects the authenticated {@code UserEntity} into a controller method parameter.
 * Resolved by {@link CurrentUserArgumentResolver}.
 *
 * <pre>{@code
 *   public ResponseEntity<UserDto> getMe(@CurrentUser UserEntity user) { ... }
 * }</pre>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUser {}

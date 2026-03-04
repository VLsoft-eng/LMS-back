package com.example.lms.repository;

import com.example.lms.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * TICKET-BE-03: Spring Data JPA repository for users.
 */
public interface UserRepository extends JpaRepository<UserEntity, UUID> {

	Optional<UserEntity> findByEmail(String email);
}

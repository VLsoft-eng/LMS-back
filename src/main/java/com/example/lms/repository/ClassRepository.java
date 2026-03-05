package com.example.lms.repository;

import com.example.lms.entity.ClassEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * TICKET-BE-03: Spring Data JPA repository for classes.
 */
public interface ClassRepository extends JpaRepository<ClassEntity, UUID> {

	@Query("SELECT c FROM ClassEntity c WHERE c.id IN (SELECT m.classId FROM ClassMemberEntity m WHERE m.userId = :userId)")
	List<ClassEntity> findAllByMembersUserId(@Param("userId") UUID userId);

	Optional<ClassEntity> findByCode(String code);

	boolean existsByCode(String code);
}

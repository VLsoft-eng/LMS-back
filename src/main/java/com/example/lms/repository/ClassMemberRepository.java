package com.example.lms.repository;

import com.example.lms.entity.ClassMemberEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * TICKET-BE-03: Spring Data JPA repository for class_members.
 */
public interface ClassMemberRepository extends JpaRepository<ClassMemberEntity, UUID> {

	Optional<ClassMemberEntity> findByClassIdAndUserId(UUID classId, UUID userId);

	Page<ClassMemberEntity> findAllByClassIdOrderByJoinedAtAsc(UUID classId, Pageable pageable);

	long countByClassId(UUID classId);
}

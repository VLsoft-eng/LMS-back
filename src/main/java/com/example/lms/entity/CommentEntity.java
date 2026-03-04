package com.example.lms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * TICKET-BE-03: JPA entity for comments table.
 */
@Entity
@Table(name = "comments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "assignment_id", nullable = false)
	private UUID assignmentId;

	@Column(name = "author_id", nullable = false)
	private UUID authorId;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String text;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@PrePersist
	void createdAt() {
		if (this.createdAt == null) {
			this.createdAt = Instant.now();
		}
	}
}

package com.example.lms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * TICKET-BE-03: JPA entity for assignments table.
 */
@Entity
@Table(name = "assignments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "class_id", nullable = false)
	private UUID classId;

	@Column(nullable = false, length = 255)
	private String title;

	@Column(columnDefinition = "TEXT")
	private String description;

	@Column(name = "created_by", nullable = false)
	private UUID createdBy;

	@Column(name = "deadline")
	private Instant deadline;

	@Column(name = "file_paths", columnDefinition = "TEXT[]")
	@org.hibernate.annotations.Array(length = 50)
	@lombok.Builder.Default
	private List<String> filePaths = new ArrayList<>();

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@PrePersist
	void createdAt() {
		if (this.createdAt == null) {
			this.createdAt = Instant.now();
		}
	}
}

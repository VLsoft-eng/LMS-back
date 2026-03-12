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
 * TICKET-BE-03: JPA entity for submissions table.
 */
@Entity
@Table(name = "submissions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "assignment_id", nullable = false)
	private UUID assignmentId;

	@Column(name = "student_id", nullable = false)
	private UUID studentId;

	@Column(name = "answer_text", columnDefinition = "TEXT")
	private String answerText;

	@Column(name = "file_paths", columnDefinition = "TEXT[]")
	@org.hibernate.annotations.Array(length = 50)
	private List<String> filePaths = new ArrayList<>();

	@Column(name = "grade")
	private Short grade;

	@Column(name = "submitted_at", nullable = false, updatable = false)
	private Instant submittedAt;

	@Column(name = "graded_at")
	private Instant gradedAt;

	@PrePersist
	void submittedAt() {
		if (this.submittedAt == null) {
			this.submittedAt = Instant.now();
		}
	}
}

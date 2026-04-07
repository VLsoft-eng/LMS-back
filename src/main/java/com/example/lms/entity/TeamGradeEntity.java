package com.example.lms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * TICKET-BE-22: JPA entity for team_grades table.
 */
@Entity
@Table(name = "team_grades", uniqueConstraints = {
		@UniqueConstraint(columnNames = {"team_id", "assignment_id"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamGradeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "team_id", nullable = false)
	private UUID teamId;

	@Column(name = "assignment_id", nullable = false)
	private UUID assignmentId;

	@Column(nullable = false)
	private Short grade;

	@Column(columnDefinition = "TEXT")
	private String comment;

	@Column(name = "graded_by", nullable = false)
	private UUID gradedBy;

	@Column(name = "graded_at", nullable = false, updatable = false)
	private Instant gradedAt;

	@PrePersist
	void gradedAt() {
		if (this.gradedAt == null) {
			this.gradedAt = Instant.now();
		}
	}
}

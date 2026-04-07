package com.example.lms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * TICKET-BE-22: JPA entity for individual_grade_adjustments table.
 */
@Entity
@Table(name = "individual_grade_adjustments", uniqueConstraints = {
		@UniqueConstraint(columnNames = {"team_grade_id", "student_id"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndividualGradeAdjustmentEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "team_grade_id", nullable = false)
	private UUID teamGradeId;

	@Column(name = "student_id", nullable = false)
	private UUID studentId;

	@Column(nullable = false)
	@Builder.Default
	private Short adjustment = 0;

	@Column(name = "final_grade", nullable = false)
	private Short finalGrade;

	@Column(columnDefinition = "TEXT")
	private String comment;

	@Column(name = "graded_by", nullable = false)
	private UUID gradedBy;

	@Column(name = "graded_at", nullable = false)
	private Instant gradedAt;

	@PrePersist
	void gradedAt() {
		if (this.gradedAt == null) {
			this.gradedAt = Instant.now();
		}
	}
}

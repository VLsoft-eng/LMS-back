package com.example.lms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * TICKET-BE-03: JPA entity for class_members table.
 */
@Entity
@Table(name = "class_members", uniqueConstraints = {
		@UniqueConstraint(columnNames = {"class_id", "user_id"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassMemberEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "class_id", nullable = false)
	private UUID classId;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Role role;

	@Column(name = "joined_at", nullable = false, updatable = false)
	private Instant joinedAt;

	@PrePersist
	void joinedAt() {
		if (this.joinedAt == null) {
			this.joinedAt = Instant.now();
		}
	}
}

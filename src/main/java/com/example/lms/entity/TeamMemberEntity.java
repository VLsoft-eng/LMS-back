package com.example.lms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * TICKET-BE-21: JPA entity for team_members table.
 */
@Entity
@Table(name = "team_members", uniqueConstraints = {
		@UniqueConstraint(columnNames = {"team_id", "user_id"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamMemberEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "team_id", nullable = false)
	private UUID teamId;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(name = "is_leader", nullable = false)
	@Builder.Default
	private boolean isLeader = false;

	@Column(name = "joined_at", nullable = false, updatable = false)
	private Instant joinedAt;

	@PrePersist
	void joinedAt() {
		if (this.joinedAt == null) {
			this.joinedAt = Instant.now();
		}
	}
}

package com.example.lms.entity.peerreview;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * TICKET #9180: конфигурация peer-review для задания.
 */
@Entity
@Table(name = "peer_review_settings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeerReviewSettingsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "assignment_id", nullable = false, unique = true)
    private UUID assignmentId;

    @Column(name = "reviews_per_student", nullable = false)
    private int reviewsPerStudent;

    @Column(name = "is_enabled", nullable = false)
    private boolean isEnabled;

    @Column(name = "due_date")
    private Instant dueDate;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}

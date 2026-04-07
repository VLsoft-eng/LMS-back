package com.example.lms.repository;

import com.example.lms.entity.TeamMemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * TICKET-BE-21: Spring Data JPA repository for team_members.
 */
public interface TeamMemberRepository extends JpaRepository<TeamMemberEntity, UUID> {

    List<TeamMemberEntity> findAllByTeamId(UUID teamId);

    Optional<TeamMemberEntity> findByTeamIdAndUserId(UUID teamId, UUID userId);

    int countByTeamId(UUID teamId);

    List<TeamMemberEntity> findAllByUserId(UUID userId);

    List<TeamMemberEntity> findAllByTeamIdIn(List<UUID> teamIds);

    void deleteAllByTeamId(UUID teamId);
}

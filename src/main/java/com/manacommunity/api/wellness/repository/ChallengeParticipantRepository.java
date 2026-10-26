package com.manacommunity.api.wellness.repository;

import com.manacommunity.api.wellness.entity.ChallengeParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChallengeParticipantRepository extends JpaRepository<ChallengeParticipant, Long> {
    Optional<ChallengeParticipant> findByChallengeIdAndUserId(Long challengeId, Long userId);
    List<ChallengeParticipant> findByChallengeId(Long challengeId);
    List<ChallengeParticipant> findByUserId(Long userId);
}

package com.manacommunity.api.wellness.repository;

import com.manacommunity.api.wellness.entity.WellnessChallenge;
import com.manacommunity.api.wellness.enums.WellnessEnums.ChallengeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WellnessChallengeRepository extends JpaRepository<WellnessChallenge, Long> {
    List<WellnessChallenge> findByStatus(ChallengeStatus status);
    List<WellnessChallenge> findByCommunityIdAndStatus(Long communityId, ChallengeStatus status);
}

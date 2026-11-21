package com.manacommunity.api.identity.portability.repository;

import com.manacommunity.api.identity.portability.entity.ResidentReputationScore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ResidentReputationScoreRepository extends JpaRepository<ResidentReputationScore, Long> {

    Optional<ResidentReputationScore> findByResidentIdAndCommunityId(Long residentId, Long communityId);

    List<ResidentReputationScore> findByCommunityIdOrderByScoreDesc(Long communityId);
}

package com.manacommunity.api.repository;

import com.manacommunity.api.model.BallotVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BallotVoteRepository extends JpaRepository<BallotVote, Long> {
    Optional<BallotVote> findByBallotIdAndUserId(Long ballotId, Long userId);
    long countByBallotId(Long ballotId);
    long countByOptionId(Long optionId);
}

package com.manacommunity.api.polling.repository;

import com.manacommunity.api.polling.entity.PollVote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PollVoteRepository extends JpaRepository<PollVote, Long> {

    List<PollVote> findByPollId(Long pollId);

    List<PollVote> findByPollIdAndVoterId(Long pollId, Long voterId);

    long countByOptionId(Long optionId);

    boolean existsByPollIdAndVoterId(Long pollId, Long voterId);
}

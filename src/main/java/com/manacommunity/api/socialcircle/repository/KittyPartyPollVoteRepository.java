package com.manacommunity.api.socialcircle.repository;

import com.manacommunity.api.socialcircle.entity.KittyPartyPollVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KittyPartyPollVoteRepository extends JpaRepository<KittyPartyPollVote, Long> {

    List<KittyPartyPollVote> findByPostId(Long postId);

    long countByPostId(Long postId);

    long countByOptionId(Long optionId);

    Optional<KittyPartyPollVote> findByPostIdAndUserId(Long postId, Long userId);

    boolean existsByPostIdAndUserId(Long postId, Long userId);

    void deleteByPostId(Long postId);
}

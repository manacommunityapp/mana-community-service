package com.manacommunity.api.polling.repository;

import com.manacommunity.api.polling.entity.Poll;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PollRepository extends JpaRepository<Poll, Long> {

    List<Poll> findByCommunityIdOrderByCreatedAtDesc(Long communityId);

    List<Poll> findByCreatedByIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT p FROM Poll p WHERE p.community.id = :communityId " +
            "AND (p.closesOn IS NULL OR p.closesOn >= CURRENT_DATE) " +
            "ORDER BY p.createdAt DESC")
    List<Poll> findActiveByCommunity(@Param("communityId") Long communityId);
}

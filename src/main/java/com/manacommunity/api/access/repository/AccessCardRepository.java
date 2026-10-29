package com.manacommunity.api.access.repository;

import com.manacommunity.api.access.entity.AccessCard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccessCardRepository extends JpaRepository<AccessCard, Long> {
    List<AccessCard> findByCommunityIdOrderByCreatedAtDesc(Long communityId);
    List<AccessCard> findByCommunityIdAndUserIdOrderByCreatedAtDesc(Long communityId, Long userId);
}

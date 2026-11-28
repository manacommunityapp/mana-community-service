package com.manacommunity.api.pins.repository;

import com.manacommunity.api.pins.entity.CommunityPin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface CommunityPinRepository extends JpaRepository<CommunityPin, Long> {

    List<CommunityPin> findByCommunityIdAndExpiresAtAfterOrderByCreatedAtDesc(Long communityId, Instant now);

    List<CommunityPin> findByCommunityIdAndCategoryAndExpiresAtAfterOrderByCreatedAtDesc(
            Long communityId, String category, Instant now);

    @Modifying
    @Query("UPDATE CommunityPin p SET p.likesCount = p.likesCount + 1, p.updatedAt = CURRENT_TIMESTAMP WHERE p.id = :id")
    int incrementLikes(@Param("id") Long id);

    @Modifying
    @Query("UPDATE CommunityPin p SET p.isClosed = true, p.closedAt = CURRENT_TIMESTAMP, p.updatedAt = CURRENT_TIMESTAMP WHERE p.id = :id")
    int closePin(@Param("id") Long id);
}

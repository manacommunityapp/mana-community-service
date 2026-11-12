package com.manacommunity.api.socialcircle.repository;

import com.manacommunity.api.socialcircle.entity.KittyPartyCircle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KittyPartyCircleRepository extends JpaRepository<KittyPartyCircle, Long> {

    @Query("""
        SELECT c FROM KittyPartyCircle c
        WHERE c.community.id = :communityId
          AND c.deletedAt IS NULL
          AND (:categoryCode IS NULL OR c.categoryCode = :categoryCode)
          AND (:status IS NULL OR c.status = :status)
        ORDER BY c.createdAt DESC
        """)
    Page<KittyPartyCircle> findByCommunity(
            @Param("communityId") Long communityId,
            @Param("categoryCode") String categoryCode,
            @Param("status") String status,
            Pageable pageable
    );

    @Query("""
        SELECT c FROM KittyPartyCircle c
        JOIN KittyPartyMember m ON m.circle.id = c.id
        WHERE m.user.id = :userId
          AND m.membershipStatus = 'ACTIVE'
          AND c.deletedAt IS NULL
        ORDER BY c.createdAt DESC
        """)
    Page<KittyPartyCircle> findCirclesByMemberUserId(@Param("userId") Long userId, Pageable pageable);

    Optional<KittyPartyCircle> findByIdAndDeletedAtIsNull(Long id);
}

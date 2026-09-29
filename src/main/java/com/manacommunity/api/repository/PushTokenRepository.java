package com.manacommunity.api.repository;

import com.manacommunity.api.model.PushToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PushTokenRepository extends JpaRepository<PushToken, Long> {

    /** All active tokens for a specific user — used for per-user push. */
    List<PushToken> findByUserIdAndActiveTrue(Long userId);

    /** All active tokens for a list of users — used for bulk push. */
    @Query("SELECT pt FROM PushToken pt WHERE pt.user.id IN :userIds AND pt.active = true")
    List<PushToken> findActiveByUserIds(@Param("userIds") List<Long> userIds);

    /**
     * All active tokens for all members of a community — used for
     * community-wide announcements and event notifications.
     */
    @Query("""
        SELECT pt FROM PushToken pt
        WHERE pt.user.community.id = :communityId
          AND pt.active = true
    """)
    List<PushToken> findActiveByCommunityId(@Param("communityId") Long communityId);

    Optional<PushToken> findByToken(String token);

    /** Soft-delete a specific token value (called on logout). */
    @Modifying
    @Query("UPDATE PushToken pt SET pt.active = false WHERE pt.token = :token")
    void deactivateByToken(@Param("token") String token);

    /** Soft-delete all tokens for a user (full logout from all devices). */
    @Modifying
    @Query("UPDATE PushToken pt SET pt.active = false WHERE pt.user.id = :userId")
    void deactivateAllByUserId(@Param("userId") Long userId);

    /** Hard-delete stale inactive tokens older than N days (run via scheduler). */
    @Modifying
    @Query("""
        DELETE FROM PushToken pt
        WHERE pt.active = false
          AND pt.updatedAt < :cutoff
    """)
    int deleteInactiveBefore(@Param("cutoff") java.time.LocalDateTime cutoff);
}

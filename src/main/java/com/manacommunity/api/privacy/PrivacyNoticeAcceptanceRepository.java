package com.manacommunity.api.privacy;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PrivacyNoticeAcceptanceRepository extends JpaRepository<PrivacyNoticeAcceptance, Long> {

    Optional<PrivacyNoticeAcceptance> findByResidentIdAndNoticeVersion(Long residentId, String noticeVersion);

    boolean existsByResidentIdAndNoticeVersion(Long residentId, String noticeVersion);

    /**
     * Returns the IDs of all residents in the community who have NOT accepted the given notice version.
     */
    @Query("""
        SELECT u.id FROM AppUser u
        WHERE u.community.id = :communityId
          AND u.id NOT IN (
              SELECT p.residentId FROM PrivacyNoticeAcceptance p
              WHERE p.communityId = :communityId
                AND p.noticeVersion = :noticeVersion
          )
        """)
    List<Long> findResidentIdsWithoutAcceptance(
            @Param("communityId") Long communityId,
            @Param("noticeVersion") String noticeVersion);
}

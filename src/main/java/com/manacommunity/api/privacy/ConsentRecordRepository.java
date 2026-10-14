package com.manacommunity.api.privacy;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConsentRecordRepository extends JpaRepository<ConsentRecord, Long> {

    List<ConsentRecord> findByUserId(Long userId);

    /** Latest record for a given user+purpose — the authoritative consent state. */
    @Query("SELECT c FROM ConsentRecord c WHERE c.userId = :userId AND c.consentPurpose = :purpose ORDER BY c.createdAt DESC LIMIT 1")
    Optional<ConsentRecord> findLatestByUserIdAndPurpose(
            @Param("userId") Long userId,
            @Param("purpose") String purpose);

    /** All consent purposes and their latest states for a user. */
    @Query("""
        SELECT c FROM ConsentRecord c
        WHERE c.userId = :userId
          AND c.createdAt = (
              SELECT MAX(c2.createdAt) FROM ConsentRecord c2
              WHERE c2.userId = c.userId AND c2.consentPurpose = c.consentPurpose
          )
        ORDER BY c.consentPurpose
        """)
    List<ConsentRecord> findLatestPerPurposeForUser(@Param("userId") Long userId);
}

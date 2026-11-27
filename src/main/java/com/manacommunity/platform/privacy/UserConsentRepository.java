package com.manacommunity.platform.privacy;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserConsentRepository extends JpaRepository<UserConsentRecord, Long> {

    /** Latest consent record for a user/purpose combination (most recent first). */
    @Query("""
            SELECT r FROM UserConsentRecord r
            WHERE r.userId = :userId AND r.purpose = :purpose
            ORDER BY r.createdAt DESC
            """)
    List<UserConsentRecord> findLatestByUserAndPurpose(
            @Param("userId") Long userId,
            @Param("purpose") ConsentPurposeCode purpose);

    default Optional<UserConsentRecord> findCurrentConsent(Long userId, ConsentPurposeCode purpose) {
        List<UserConsentRecord> records = findLatestByUserAndPurpose(userId, purpose);
        return records.isEmpty() ? Optional.empty() : Optional.of(records.get(0));
    }

    List<UserConsentRecord> findByUserIdOrderByCreatedAtDesc(Long userId);
}

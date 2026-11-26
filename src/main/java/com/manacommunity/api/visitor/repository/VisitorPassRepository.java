package com.manacommunity.api.visitor.repository;

import com.manacommunity.api.visitor.entity.VisitorPass;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface VisitorPassRepository extends JpaRepository<VisitorPass, Long> {

    Optional<VisitorPass> findByPassCode(String passCode);

    Optional<VisitorPass> findByCommunityIdAndIdempotencyKey(Long communityId, String idempotencyKey);

    Optional<VisitorPass> findByIdempotencyKey(String idempotencyKey);

    @Query("""
           SELECT v FROM VisitorPass v
           WHERE v.status = :status
             AND (v.resident.id = :residentId
                  OR (:communityId IS NOT NULL
                      AND :flatNumber IS NOT NULL
                      AND v.community.id = :communityId
                      AND LOWER(TRIM(v.flatNumber)) = LOWER(TRIM(:flatNumber))))
           ORDER BY v.createdAt DESC
           """)
    List<VisitorPass> findPendingForResidentOrFlat(
            @Param("residentId") Long residentId,
            @Param("communityId") Long communityId,
            @Param("flatNumber") String flatNumber,
            @Param("status") VisitorPass.PassStatus status);

    /** Looks up a pass by the SHA-256 hash of its OTP. Never pass the plain OTP here. */
    Optional<VisitorPass> findByOtpHash(String otpHash);

    Optional<VisitorPass> findFirstByVisitorPhoneAndStatusInOrderByCreatedAtDesc(
            String phone, List<VisitorPass.PassStatus> statuses);

    List<VisitorPass> findByResidentIdAndStatusOrderByCreatedAtDesc(
            Long residentId, VisitorPass.PassStatus status);

    List<VisitorPass> findByCommunityIdAndStatusOrderByCreatedAtDesc(
            Long communityId, VisitorPass.PassStatus status);

    List<VisitorPass> findByResidentIdOrderByCreatedAtDesc(Long residentId);

    List<VisitorPass> findByCommunityIdOrderByCreatedAtDesc(Long communityId);

    @Query("SELECT v FROM VisitorPass v WHERE v.community.id = :communityId " +
           "AND v.status IN :statuses ORDER BY v.createdAt DESC")
    List<VisitorPass> findByCommunityAndStatuses(
            @Param("communityId") Long communityId,
            @Param("statuses") List<VisitorPass.PassStatus> statuses);

    @Query("SELECT v FROM VisitorPass v WHERE v.community.id = :communityId " +
           "AND v.createdAt >= :since ORDER BY v.createdAt DESC")
    List<VisitorPass> findRecentByCommunity(
            @Param("communityId") Long communityId,
            @Param("since") LocalDateTime since);

    long countByCommunityIdAndStatus(Long communityId, VisitorPass.PassStatus status);

    @Query("SELECT COUNT(v) FROM VisitorPass v WHERE v.community.id = :cid " +
           "AND v.expectedAt >= :start AND v.expectedAt < :end " +
           "AND v.status IN (com.manacommunity.api.visitor.entity.VisitorPass.PassStatus.APPROVED, " +
           "com.manacommunity.api.visitor.entity.VisitorPass.PassStatus.EXPECTED)")
    long countExpectedToday(@Param("cid") Long communityId,
                            @Param("start") LocalDateTime start,
                            @Param("end") LocalDateTime end);

    long countByCommunityId(Long communityId);

    @Query("SELECT COUNT(v) FROM VisitorPass v WHERE v.community.id = :cid AND v.expectedAt >= :start AND v.expectedAt < :end")
    long countTodayVisitors(@Param("cid") Long communityId,
                            @Param("start") LocalDateTime start,
                            @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(v) FROM VisitorPass v WHERE v.community.id = :cid " +
           "AND v.status = com.manacommunity.api.visitor.entity.VisitorPass.PassStatus.CHECKED_IN " +
           "AND v.vehicleNumber IS NOT NULL AND v.vehicleNumber != ''")
    long countVehiclesIn(@Param("cid") Long communityId);

    @Query("SELECT COUNT(v) FROM VisitorPass v WHERE v.community.id = :cid " +
           "AND v.expectedAt >= :start AND v.expectedAt < :end " +
           "AND (v.passType = com.manacommunity.api.visitor.entity.VisitorPass.PassType.DELIVERY " +
           "OR LOWER(v.purpose) LIKE '%delivery%')")
    long countTodayDeliveries(@Param("cid") Long communityId,
                              @Param("start") LocalDateTime start,
                              @Param("end") LocalDateTime end);

    @Query("""
           SELECT v FROM VisitorPass v 
           WHERE v.community.id = :communityId 
             AND v.status IN :statuses
             AND REPLACE(REPLACE(UPPER(v.vehicleNumber), ' ', ''), '-', '') = :normalizedPlate
           ORDER BY v.createdAt DESC
           """)
    List<VisitorPass> findActiveByCommunityAndNormalizedVehicle(
            @Param("communityId") Long communityId,
            @Param("statuses") List<VisitorPass.PassStatus> statuses,
            @Param("normalizedPlate") String normalizedPlate);

    /**
     * Atomic Compare-and-Swap (CAS) status transition executed under PostgreSQL
     * row-level lock. Guarantees at most one concurrent pod/resident succeeds
     * when Approve and Deny arrive at the exact same millisecond.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
           UPDATE VisitorPass v
              SET v.status = :targetStatus,
                  v.updatedAt = :updatedAt,
                  v.version = COALESCE(v.version, 0) + 1
            WHERE v.id = :id
              AND v.status = :expectedStatus
           """)
    int compareAndSwapStatus(
            @Param("id") Long id,
            @Param("expectedStatus") VisitorPass.PassStatus expectedStatus,
            @Param("targetStatus") VisitorPass.PassStatus targetStatus,
            @Param("updatedAt") LocalDateTime updatedAt);

    /**
     * Keyset cursor lookup of IDs eligible for DPDP retention (zero heap bloat from Base64 photos).
     */
    @Query("""
           SELECT v.id FROM VisitorPass v
            WHERE (:communityId IS NULL OR v.community.id = :communityId)
              AND v.createdAt < :cutoff
              AND v.status IN :terminalStatuses
              AND v.id > :cursorId
              AND (:includeAlreadyRedacted = true OR COALESCE(v.visitorPhone, '') != 'REDACTED')
            ORDER BY v.id ASC
           """)
    List<Long> findRetentionCandidateIds(
            @Param("communityId") Long communityId,
            @Param("cutoff") LocalDateTime cutoff,
            @Param("terminalStatuses") List<VisitorPass.PassStatus> terminalStatuses,
            @Param("cursorId") Long cursorId,
            @Param("includeAlreadyRedacted") boolean includeAlreadyRedacted,
            Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
           UPDATE VisitorPass v
              SET v.visitorPhone = 'REDACTED',
                  v.visitorPhoto = null,
                  v.vehicleNumber = null
            WHERE v.id IN :ids
           """)
    int anonymizePassBatch(@Param("ids") List<Long> ids);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM VisitorPass v WHERE v.id IN :ids")
    int deletePassBatch(@Param("ids") List<Long> ids);
}

package com.manacommunity.api.repository.karate;

import com.manacommunity.api.model.karate.SportsKarateEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SportsKarateEnrollmentRepository extends JpaRepository<SportsKarateEnrollment, Long> {

    List<SportsKarateEnrollment> findByBatchId(Long batchId);

    List<SportsKarateEnrollment> findByBatchIdAndStatus(Long batchId, SportsKarateEnrollment.EnrollmentStatus status);

    List<SportsKarateEnrollment> findByStudentIdAndStatus(Long studentId, SportsKarateEnrollment.EnrollmentStatus status);

    List<SportsKarateEnrollment> findByStudentId(Long studentId);

    Optional<SportsKarateEnrollment> findByBatchIdAndStudentId(Long batchId, Long studentId);

    boolean existsByBatchIdAndStudentId(Long batchId, Long studentId);

    /** All active enrollments eligible for grading in a batch. */
    List<SportsKarateEnrollment> findByBatchIdAndStatusAndGradingEligibleTrue(
            Long batchId, SportsKarateEnrollment.EnrollmentStatus status);

    /** Active enrollments below attendance threshold for alert job. */
    @Query("""
            SELECT e FROM SportsKarateEnrollment e
            WHERE e.batch.id = :batchId
              AND e.status = 'ACTIVE'
              AND e.attendancePercentage < :threshold
            """)
    List<SportsKarateEnrollment> findBelowThreshold(@Param("batchId") Long batchId,
                                                    @Param("threshold") java.math.BigDecimal threshold);

    /** All active enrollments in a community (used by nightly eligibility engine). */
    @Query("""
            SELECT e FROM SportsKarateEnrollment e
            WHERE e.community.id = :communityId
              AND e.status = 'ACTIVE'
            """)
    List<SportsKarateEnrollment> findActiveForCommunity(@Param("communityId") Long communityId);
}

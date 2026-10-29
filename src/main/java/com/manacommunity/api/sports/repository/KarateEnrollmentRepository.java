package com.manacommunity.api.sports.repository;

import com.manacommunity.api.sports.model.KarateEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KarateEnrollmentRepository extends JpaRepository<KarateEnrollment, Long> {
    List<KarateEnrollment> findByBatchIdOrderByEnrolledAtAsc(Long batchId);
    Optional<KarateEnrollment> findByBatchIdAndUserId(Long batchId, Long userId);
    boolean existsByBatchIdAndUserId(Long batchId, Long userId);

    @Query("SELECT COUNT(e) FROM KarateEnrollment e WHERE e.batch.id = :batchId AND e.status = 'ACTIVE'")
    long countActiveEnrollments(@Param("batchId") Long batchId);
}

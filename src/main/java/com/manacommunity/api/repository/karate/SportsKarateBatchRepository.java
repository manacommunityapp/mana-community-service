package com.manacommunity.api.repository.karate;

import com.manacommunity.api.model.karate.SportsKarateBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SportsKarateBatchRepository extends JpaRepository<SportsKarateBatch, Long> {

    List<SportsKarateBatch> findByProgramIdOrderByBatchNameAsc(Long programId);

    List<SportsKarateBatch> findByCommunityIdAndStatus(Long communityId, SportsKarateBatch.BatchStatus status);

    List<SportsKarateBatch> findByProgramIdAndStatus(Long programId, SportsKarateBatch.BatchStatus status);

    @Query("SELECT COUNT(e) FROM SportsKarateEnrollment e WHERE e.batch.id = :batchId AND e.status = 'ACTIVE'")
    long countActiveEnrollments(@Param("batchId") Long batchId);
}

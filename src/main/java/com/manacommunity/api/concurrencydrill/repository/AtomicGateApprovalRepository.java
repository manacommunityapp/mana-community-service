package com.manacommunity.api.concurrencydrill.repository;

import com.manacommunity.api.concurrencydrill.entity.AtomicGateApproval;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface AtomicGateApprovalRepository extends JpaRepository<AtomicGateApproval, Long> {

    /**
     * Atomic Compare-And-Set state transition preventing TOCTOU double-approval races.
     */
    @Modifying
    @Query("""
        UPDATE AtomicGateApproval g
           SET g.status = :newStatus,
               g.decidedBy = :actorId,
               g.decidedAt = :decidedAt,
               g.version = g.version + 1
         WHERE g.id = :id
           AND g.status = 'PENDING'
    """)
    int transitionFromPending(
            @Param("id") Long id,
            @Param("newStatus") String newStatus,
            @Param("actorId") Long actorId,
            @Param("decidedAt") Instant decidedAt
    );
}

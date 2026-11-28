package com.manacommunity.api.concurrencydrill.repository;

import com.manacommunity.api.concurrencydrill.entity.AtomicErpReceipt;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface AtomicErpReceiptRepository extends JpaRepository<AtomicErpReceipt, Long> {

    record ReceiptExportRow(
            Long receiptId,
            String flatNo,
            long amountPaise,
            String paymentMode,
            Instant paidAt
    ) {}

    /**
     * Keyset cursor projection query — avoids N+1 loops, avoids OFFSET scan degradation,
     * and avoids Hibernate L1 entity graph retention.
     */
    @Query("""
        SELECT new com.manacommunity.api.concurrencydrill.repository.AtomicErpReceiptRepository$ReceiptExportRow(
            r.id, r.flatNo, r.amountPaise, r.paymentMode, r.paidAt
        )
          FROM AtomicErpReceipt r
         WHERE r.communityId = :communityId
           AND r.paidAt >= :fromTime
           AND r.paidAt <= :toTime
           AND r.id > :cursorId
         ORDER BY r.id ASC
    """)
    List<ReceiptExportRow> fetchExportBatch(
            @Param("communityId") Long communityId,
            @Param("fromTime") Instant fromTime,
            @Param("toTime") Instant toTime,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );
}

package com.manacommunity.api.repository;

import com.manacommunity.api.model.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Page<Invoice> findByCommunityIdOrderByGeneratedAtDesc(Long communityId, Pageable pageable);

    Page<Invoice> findByCommunityIdAndStatusOrderByGeneratedAtDesc(
            Long communityId, String status, Pageable pageable);

    List<Invoice> findByResidentIdOrderByGeneratedAtDesc(Long residentId);

    List<Invoice> findByExpenseId(Long expenseId);

    long countByCommunityIdAndStatus(Long communityId, String status);

    java.util.Optional<Invoice> findByIdAndCommunityId(Long id, Long communityId);

    List<Invoice> findByCommunityId(Long communityId);

    long countByCommunityId(Long communityId);

    @Query("SELECT COUNT(i) FROM Invoice i WHERE i.community.id = :cid " +
           "AND i.status IN ('UNPAID','OVERDUE') AND i.dueDate < :today")
    long countOverdue(@Param("cid") Long communityId, @Param("today") LocalDate today);

    @Query("SELECT COALESCE(SUM(i.totalAmount), 0) FROM Invoice i WHERE i.community.id = :cid " +
           "AND i.status IN ('UNPAID','OVERDUE') AND i.dueDate < :today")
    java.math.BigDecimal sumOverdueAmount(@Param("cid") Long communityId, @Param("today") LocalDate today);

    @Query("""
            SELECT COALESCE(SUM(CASE WHEN i.status = 'PARTIAL' THEN
                GREATEST(i.totalAmount - COALESCE((
                    SELECT SUM(p.amount - p.refundedAmount) FROM PaymentOrder p
                    WHERE p.communityId = :cid AND p.referenceId = i.id
                      AND p.referenceType = com.manacommunity.api.payments.model.PaymentReferenceType.MAINTENANCE_BILL
                      AND p.status IN (com.manacommunity.api.payments.model.PaymentOrderStatus.SUCCESS,
                                       com.manacommunity.api.payments.model.PaymentOrderStatus.PARTIALLY_REFUNDED)
                ), 0), 0)
                ELSE i.totalAmount END), 0)
            FROM Invoice i
            WHERE i.community.id = :cid AND i.status IN ('UNPAID','OVERDUE','PARTIAL')
            """)
    java.math.BigDecimal sumOutstandingAmount(@Param("cid") Long communityId);

    @Query("SELECT COUNT(i) FROM Invoice i WHERE i.community.id = :cid " +
           "AND i.status IN ('UNPAID','OVERDUE','PARTIAL')")
    long countOutstanding(@Param("cid") Long communityId);

    @Query("SELECT COALESCE(SUM(i.totalAmount), 0) FROM Invoice i WHERE i.community.id = :cid " +
           "AND i.expense IS NULL AND i.status = 'PAID' AND i.paidAt BETWEEN :from AND :to")
    java.math.BigDecimal sumCollectedBetween(@Param("cid") Long communityId,
                                              @Param("from") LocalDateTime from,
                                              @Param("to") LocalDateTime to);

    java.util.List<Invoice> findByCommunityIdAndGeneratedAtBetween(
            Long communityId,
            java.time.LocalDateTime from,
            java.time.LocalDateTime to);
}

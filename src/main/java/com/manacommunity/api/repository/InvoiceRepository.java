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

    /** All invoices for a community (used to aggregate overdueAmount). */
    List<Invoice> findByCommunityId(Long communityId);

    /** Invoices for a community generated within the given datetime range. */
    List<Invoice> findByCommunityIdAndGeneratedAtBetween(
            Long communityId, LocalDateTime from, LocalDateTime to);

    /** Count invoices that are UNPAID and past their due date. */
    @Query("SELECT COUNT(i) FROM Invoice i WHERE i.community.id = :communityId " +
           "AND i.status = 'UNPAID' AND i.dueDate < :today")
    long countOverdue(@Param("communityId") Long communityId, @Param("today") LocalDate today);
}

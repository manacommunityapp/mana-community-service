package com.manacommunity.api.repository;

import com.manacommunity.api.model.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
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

    java.util.List<Invoice> findByCommunityIdAndGeneratedAtBetween(
            Long communityId,
            java.time.LocalDateTime from,
            java.time.LocalDateTime to);
}

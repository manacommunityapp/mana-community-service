package com.manacommunity.api.repository;

import com.manacommunity.api.model.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Page<Invoice> findByCommunityIdOrderByGeneratedAtDesc(Long communityId, Pageable pageable);

    Page<Invoice> findByCommunityIdAndStatusOrderByGeneratedAtDesc(
            Long communityId, String status, Pageable pageable);

    List<Invoice> findByResidentIdOrderByGeneratedAtDesc(Long residentId);

    List<Invoice> findByExpenseId(Long expenseId);

    long countByCommunityIdAndStatus(Long communityId, String status);
}

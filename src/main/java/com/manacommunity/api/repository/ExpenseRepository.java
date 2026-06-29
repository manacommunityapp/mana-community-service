package com.manacommunity.api.repository;

import com.manacommunity.api.model.Expense;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    Page<Expense> findByCommunityIdOrderByCreatedAtDesc(Long communityId, Pageable pageable);

    Page<Expense> findByCommunityIdAndStatusOrderByCreatedAtDesc(
            Long communityId, String status, Pageable pageable);
}

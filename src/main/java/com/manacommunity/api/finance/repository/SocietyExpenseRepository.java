package com.manacommunity.api.finance.repository;

import com.manacommunity.api.finance.entity.SocietyExpense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SocietyExpenseRepository extends JpaRepository<SocietyExpense, Long> {

    List<SocietyExpense> findByCommunityIdOrderByCreatedAtDesc(Long communityId);

    List<SocietyExpense> findByCommunityIdAndStatusOrderByCreatedAtDesc(Long communityId, SocietyExpense.ApprovalStatus status);

    long countByCommunityIdAndStatusIn(Long communityId, List<SocietyExpense.ApprovalStatus> statuses);
}

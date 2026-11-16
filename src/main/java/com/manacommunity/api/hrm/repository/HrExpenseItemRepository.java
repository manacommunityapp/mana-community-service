package com.manacommunity.api.hrm.repository;

import com.manacommunity.api.hrm.entity.HrExpenseItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface HrExpenseItemRepository extends JpaRepository<HrExpenseItem, Long> {
    List<HrExpenseItem> findByExpenseClaimId(Long claimId);
}

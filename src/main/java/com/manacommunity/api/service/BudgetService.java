package com.manacommunity.api.service;

import com.manacommunity.api.dto.BudgetAllocationRequest;
import com.manacommunity.api.model.BudgetAllocation;
import com.manacommunity.api.repository.BudgetAllocationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class BudgetService {

    private final BudgetAllocationRepository budgetAllocationRepository;

    public BudgetService(BudgetAllocationRepository budgetAllocationRepository) {
        this.budgetAllocationRepository = budgetAllocationRepository;
    }

    public List<BudgetAllocation> getAllocations(Long communityId, String financialYear) {
        return budgetAllocationRepository.findByCommunityIdAndFinancialYearOrderByCategoryAsc(communityId, financialYear);
    }

    @Transactional
    public BudgetAllocation upsertAllocation(Long communityId, BudgetAllocationRequest request) {
        BudgetAllocation allocation = budgetAllocationRepository
                .findByCommunityIdAndFinancialYearAndCategory(communityId, request.getFinancialYear(), request.getCategory())
                .orElseGet(BudgetAllocation::new);
        allocation.setCommunityId(communityId);
        allocation.setFinancialYear(request.getFinancialYear());
        allocation.setCategory(request.getCategory());
        allocation.setAllocatedAmount(request.getAllocatedAmount());
        if (allocation.getSpentAmount() == null) {
            allocation.setSpentAmount(BigDecimal.ZERO);
        }
        allocation.setNotes(request.getNotes());
        return budgetAllocationRepository.save(allocation);
    }
}

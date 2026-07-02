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

    public List<BudgetAllocation> getAllocations(String financialYear) {
        return budgetAllocationRepository.findByFinancialYearOrderByCategoryAsc(financialYear);
    }

    @Transactional
    public BudgetAllocation upsertAllocation(BudgetAllocationRequest request) {
        BudgetAllocation allocation = budgetAllocationRepository
                .findByFinancialYearAndCategory(request.getFinancialYear(), request.getCategory())
                .orElseGet(BudgetAllocation::new);
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

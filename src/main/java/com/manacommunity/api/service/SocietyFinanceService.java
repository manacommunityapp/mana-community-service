package com.manacommunity.api.service;

import com.manacommunity.api.dto.SocietyDueRequest;
import com.manacommunity.api.dto.SocietyExpenseRequest;
import com.manacommunity.api.model.SocietyDue;
import com.manacommunity.api.model.SocietyExpenseRecord;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface SocietyFinanceService {

    // Dues
    List<SocietyDue> getDues(Long communityId, String status, String flat);
    SocietyDue createDue(Long communityId, SocietyDueRequest request);
    SocietyDue markDuePaid(Long communityId, Long id, String receiptUrl);
    Map<String, Object> getDuesSummary(Long communityId);

    // Expenses
    List<SocietyExpenseRecord> getExpenses(Long communityId, String category, LocalDate from, LocalDate to);
    SocietyExpenseRecord createExpense(Long communityId, SocietyExpenseRequest request);
    Map<String, Object> getExpensesSummary(Long communityId);

    // Balance sheet
    Map<String, Object> getBalanceSheet(Long communityId, Integer month, Integer year);
}

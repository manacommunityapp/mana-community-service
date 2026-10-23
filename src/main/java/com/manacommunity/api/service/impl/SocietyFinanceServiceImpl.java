package com.manacommunity.api.service.impl;

import com.manacommunity.api.dto.SocietyDueRequest;
import com.manacommunity.api.dto.SocietyExpenseRequest;
import com.manacommunity.api.model.SocietyDue;
import com.manacommunity.api.model.SocietyExpenseRecord;
import com.manacommunity.api.repository.SocietyDueRepository;
import com.manacommunity.api.repository.SocietyExpenseRecordRepository;
import com.manacommunity.api.service.SocietyFinanceService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SocietyFinanceServiceImpl implements SocietyFinanceService {

    private final SocietyDueRepository dueRepository;
    private final SocietyExpenseRecordRepository expenseRepository;
    private final AppUserRepository appUserRepository;

    // ── Dues ─────────────────────────────────────────────────────────────

    @Override
    public List<SocietyDue> getDues(Long communityId, String status, String flat) {
        if (status != null && !status.isBlank()) {
            return dueRepository.findByCommunityIdAndStatusOrderByDueDateDesc(
                    communityId, SocietyDue.DueStatus.valueOf(status.toUpperCase()));
        }
        if (flat != null && !flat.isBlank()) {
            return dueRepository.findByCommunityIdAndFlatNumberOrderByDueDateDesc(communityId, flat);
        }
        return dueRepository.findByCommunityIdOrderByDueDateDesc(communityId);
    }

    @Override
    @Transactional
    public SocietyDue createDue(Long communityId, SocietyDueRequest request) {
        SocietyDue due = SocietyDue.builder()
                .communityId(communityId)
                .flatNumber(request.flatNumber())
                .amount(request.amount())
                .dueDate(request.dueDate())
                .category(SocietyDue.DueCategory.valueOf(request.category().toUpperCase()))
                .period(request.period())
                .build();

        if (request.residentUserId() != null) {
            AppUser resident = appUserRepository.findById(request.residentUserId()).orElse(null);
            due.setResidentUser(resident);
        }

        return dueRepository.save(due);
    }

    @Override
    @Transactional
    public SocietyDue markDuePaid(Long communityId, Long id, String receiptUrl) {
        SocietyDue due = dueRepository.findByIdAndCommunityId(id, communityId)
                .orElseThrow(() -> new IllegalArgumentException("Due not found: " + id));
        due.setStatus(SocietyDue.DueStatus.PAID);
        due.setPaidDate(LocalDate.now());
        due.setReceiptUrl(receiptUrl);
        return dueRepository.save(due);
    }

    @Override
    public Map<String, Object> getDuesSummary(Long communityId) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalPending", dueRepository.sumByCommunityIdAndStatus(communityId, SocietyDue.DueStatus.PENDING));
        summary.put("totalPaid", dueRepository.sumByCommunityIdAndStatus(communityId, SocietyDue.DueStatus.PAID));
        summary.put("totalOverdue", dueRepository.sumByCommunityIdAndStatus(communityId, SocietyDue.DueStatus.OVERDUE));
        summary.put("pendingCount", dueRepository.countByCommunityIdAndStatus(communityId, SocietyDue.DueStatus.PENDING));
        summary.put("overdueCount", dueRepository.countByCommunityIdAndStatus(communityId, SocietyDue.DueStatus.OVERDUE));
        return summary;
    }

    // ── Expenses ─────────────────────────────────────────────────────────

    @Override
    public List<SocietyExpenseRecord> getExpenses(Long communityId, String category, LocalDate from, LocalDate to) {
        if (category != null && !category.isBlank()) {
            return expenseRepository.findByCommunityIdAndCategoryOrderByDateDesc(communityId, category);
        }
        if (from != null && to != null) {
            return expenseRepository.findByCommunityIdAndDateBetweenOrderByDateDesc(communityId, from, to);
        }
        return expenseRepository.findByCommunityIdOrderByDateDesc(communityId);
    }

    @Override
    @Transactional
    public SocietyExpenseRecord createExpense(Long communityId, SocietyExpenseRequest request) {
        SocietyExpenseRecord expense = SocietyExpenseRecord.builder()
                .communityId(communityId)
                .title(request.title())
                .amount(request.amount())
                .category(request.category())
                .vendor(request.vendor())
                .approvedBy(request.approvedBy())
                .date(request.date())
                .receiptUrl(request.receiptUrl())
                .notes(request.notes())
                .build();
        return expenseRepository.save(expense);
    }

    @Override
    public Map<String, Object> getExpensesSummary(Long communityId) {
        LocalDate now = LocalDate.now();
        BigDecimal thisMonth = expenseRepository.sumByCommunityIdAndMonthYear(
                communityId, now.getMonthValue(), now.getYear());

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalThisMonth", thisMonth);
        summary.put("month", now.getMonthValue());
        summary.put("year", now.getYear());
        return summary;
    }

    // ── Balance Sheet ────────────────────────────────────────────────────

    @Override
    public Map<String, Object> getBalanceSheet(Long communityId, Integer month, Integer year) {
        LocalDate now = LocalDate.now();
        int m = month != null ? month : now.getMonthValue();
        int y = year != null ? year : now.getYear();

        BigDecimal totalCollected = dueRepository.sumByCommunityIdAndStatus(communityId, SocietyDue.DueStatus.PAID);
        BigDecimal totalExpenses = expenseRepository.sumByCommunityIdAndMonthYear(communityId, m, y);
        BigDecimal balance = totalCollected.subtract(totalExpenses);

        Map<String, Object> sheet = new HashMap<>();
        sheet.put("month", m);
        sheet.put("year", y);
        sheet.put("totalCollected", totalCollected);
        sheet.put("totalExpenses", totalExpenses);
        sheet.put("balance", balance);
        return sheet;
    }
}

package com.manacommunity.api.service;

import com.manacommunity.api.dto.ExpenseRequest;
import com.manacommunity.api.dto.ExpenseResponse;
import com.manacommunity.api.dto.ExpenseSummaryResponse;
import com.manacommunity.api.dto.PagedResponse;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.model.Expense;
import com.manacommunity.api.repository.ExpenseRepository;
import com.manacommunity.api.user.model.AppUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Business layer for community expenses. Every operation is scoped to the
 * caller's community, so an admin can never read or mutate another community's
 * expenses. Approving an expense hands off to {@link BillingService} to generate
 * per-resident invoices (asynchronously), preserving the existing billing flow.
 */
@Service
public class ExpenseService {

    private static final Logger log = LoggerFactory.getLogger(ExpenseService.class);

    /** Expense lifecycle states (kept as strings to match the entity column). */
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";

    private static final int MAX_PAGE_SIZE = 50;

    private final ExpenseRepository expenseRepository;
    private final BillingService billingService;

    public ExpenseService(ExpenseRepository expenseRepository, BillingService billingService) {
        this.expenseRepository = expenseRepository;
        this.billingService = billingService;
    }

    // ── Reads ───────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PagedResponse<ExpenseResponse> list(Long communityId, String status, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), clampSize(size));
        Page<Expense> result = (status != null && !status.isBlank())
                ? expenseRepository.findByCommunityIdAndStatusOrderByCreatedAtDesc(
                        communityId, status.toUpperCase(), pageable)
                : expenseRepository.findByCommunityIdOrderByCreatedAtDesc(communityId, pageable);
        return PagedResponse.from(result, ExpenseResponse::from);
    }

    @Transactional(readOnly = true)
    public ExpenseResponse get(Long id, Long communityId) {
        return ExpenseResponse.from(require(id, communityId));
    }

    @Transactional(readOnly = true)
    public ExpenseSummaryResponse summary(Long communityId) {
        return new ExpenseSummaryResponse(
                expenseRepository.countByCommunityId(communityId),
                expenseRepository.sumAmountByCommunity(communityId),
                expenseRepository.countByCommunityIdAndStatus(communityId, STATUS_PENDING),
                expenseRepository.sumAmountByCommunityAndStatus(communityId, STATUS_PENDING),
                expenseRepository.countByCommunityIdAndStatus(communityId, STATUS_APPROVED),
                expenseRepository.sumAmountByCommunityAndStatus(communityId, STATUS_APPROVED),
                expenseRepository.countByCommunityIdAndStatus(communityId, STATUS_REJECTED),
                expenseRepository.sumAmountByCommunityAndStatus(communityId, STATUS_REJECTED)
        );
    }

    // ── Writes ──────────────────────────────────────────────────────────────

    @Transactional
    public ExpenseResponse create(ExpenseRequest req, MultipartFile receipt, AppUser creator) {
        Expense expense = Expense.builder()
                .title(req.title().trim())
                .amount(req.amount())
                .category(req.category().trim())
                .description(req.description())
                .status(STATUS_PENDING)
                .community(creator.getCommunity())
                .createdBy(creator)
                .build();

        applyReceipt(expense, receipt);

        expense = expenseRepository.save(expense);
        log.info("Expense created: id={} community={} amount={} by user={}",
                expense.getId(), creator.getCommunity().getId(), expense.getAmount(), creator.getId());
        return ExpenseResponse.from(expense);
    }

    @Transactional
    public ExpenseResponse update(Long id, ExpenseRequest req, MultipartFile receipt, Long communityId) {
        Expense expense = require(id, communityId);
        if (!STATUS_PENDING.equalsIgnoreCase(expense.getStatus())) {
            throw new IllegalStateException("Only PENDING expenses can be edited (current: "
                    + expense.getStatus() + ")");
        }
        expense.setTitle(req.title().trim());
        expense.setAmount(req.amount());
        expense.setCategory(req.category().trim());
        expense.setDescription(req.description());
        applyReceipt(expense, receipt);
        return ExpenseResponse.from(expenseRepository.save(expense));
    }

    @Transactional
    public ExpenseResponse approve(Long id, Long communityId) {
        Expense expense = require(id, communityId);
        expense.setStatus(STATUS_APPROVED);
        expenseRepository.save(expense);

        // Preserve the existing billing flow: fan out per-resident invoices.
        billingService.batchGenerateInvoices(expense.getId());
        log.info("Expense approved: id={} community={} — invoice generation triggered", id, communityId);
        return ExpenseResponse.from(expense);
    }

    @Transactional
    public ExpenseResponse reject(Long id, Long communityId) {
        Expense expense = require(id, communityId);
        expense.setStatus(STATUS_REJECTED);
        return ExpenseResponse.from(expenseRepository.save(expense));
    }

    @Transactional
    public void delete(Long id, Long communityId) {
        Expense expense = require(id, communityId);
        if (STATUS_APPROVED.equalsIgnoreCase(expense.getStatus())) {
            throw new IllegalStateException("Approved expenses cannot be deleted (invoices may exist)");
        }
        expenseRepository.delete(expense);
        log.info("Expense deleted: id={} community={}", id, communityId);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /** Loads an expense and asserts it belongs to the caller's community. */
    private Expense require(Long id, Long communityId) {
        return expenseRepository.findByIdAndCommunityId(id, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense", id));
    }

    private void applyReceipt(Expense expense, MultipartFile receipt) {
        if (receipt == null || receipt.isEmpty()) {
            return;
        }
        expense.setReceiptFileName(receipt.getOriginalFilename());
        // Mock — in production, upload to S3 and store the returned object URL.
        expense.setReceiptUrl("https://mana-community-receipts.s3.ap-south-1.amazonaws.com/receipts/"
                + receipt.getOriginalFilename());
    }

    private int clampSize(int size) {
        return Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
    }
}

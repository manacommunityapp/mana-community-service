package com.manacommunity.api.service;

import com.manacommunity.api.model.*;
import com.manacommunity.api.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class LedgerService {

    private static final Logger log = LoggerFactory.getLogger(LedgerService.class);

    private final LedgerAccountRepository accountRepo;
    private final LedgerEntryRepository entryRepo;
    private final CommunityRepository communityRepo;

    public LedgerService(LedgerAccountRepository accountRepo,
                         LedgerEntryRepository entryRepo,
                         CommunityRepository communityRepo) {
        this.accountRepo = accountRepo;
        this.entryRepo = entryRepo;
        this.communityRepo = communityRepo;
    }

    @Transactional
    public void initializeChartOfAccounts(Long communityId) {
        Community community = communityRepo.findById(communityId)
                .orElseThrow(() -> new IllegalArgumentException("Community not found: " + communityId));

        List<LedgerAccount> existing = accountRepo.findByCommunityIdAndIsSystemAccountTrue(communityId);
        if (!existing.isEmpty()) {
            log.info("Chart of accounts already initialized for community {}", communityId);
            return;
        }

        List<String[]> systemAccounts = List.of(
            new String[]{"1000", "Bank Account", "ASSET"},
            new String[]{"1100", "Accounts Receivable - Maintenance", "ASSET"},
            new String[]{"1200", "Advance Deposits", "ASSET"},
            new String[]{"1300", "Fixed Deposits", "ASSET"},
            new String[]{"2000", "Accounts Payable - Vendors", "LIABILITY"},
            new String[]{"2100", "GST Payable", "LIABILITY"},
            new String[]{"2200", "Security Deposits Held", "LIABILITY"},
            new String[]{"3000", "Society Corpus Fund", "EQUITY"},
            new String[]{"3100", "Sinking Fund Reserve", "EQUITY"},
            new String[]{"4000", "Maintenance Income", "REVENUE"},
            new String[]{"4100", "Penalty Income", "REVENUE"},
            new String[]{"4200", "Parking Income", "REVENUE"},
            new String[]{"4300", "Interest Income", "REVENUE"},
            new String[]{"5000", "Utility Expenses", "EXPENSE"},
            new String[]{"5100", "Staff Salary", "EXPENSE"},
            new String[]{"5200", "Repair & Maintenance", "EXPENSE"},
            new String[]{"5300", "Generator Fuel", "EXPENSE"},
            new String[]{"5400", "Water Tanker", "EXPENSE"},
            new String[]{"5500", "Insurance Premium", "EXPENSE"},
            new String[]{"5600", "Legal & Professional Fees", "EXPENSE"},
            new String[]{"5700", "Bank Charges", "EXPENSE"}
        );

        for (String[] acct : systemAccounts) {
            accountRepo.save(LedgerAccount.builder()
                    .community(community)
                    .accountCode(acct[0])
                    .accountName(acct[1])
                    .accountType(acct[2])
                    .isSystemAccount(true)
                    .currentBalance(BigDecimal.ZERO)
                    .active(true)
                    .build());
        }
        log.info("Initialized {} system accounts for community {}", systemAccounts.size(), communityId);
    }

    @Transactional
    public String postJournalEntry(Long communityId, List<JournalLine> lines, String narration,
                                    String referenceType, Long referenceId, Long postedBy) {
        BigDecimal totalDebits = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;

        for (JournalLine line : lines) {
            if ("DEBIT".equals(line.entryType())) {
                totalDebits = totalDebits.add(line.amount());
            } else {
                totalCredits = totalCredits.add(line.amount());
            }
        }

        if (totalDebits.compareTo(totalCredits) != 0) {
            throw new IllegalArgumentException(
                    String.format("Journal entry unbalanced: debits=%s credits=%s", totalDebits, totalCredits));
        }

        Community community = communityRepo.findById(communityId)
                .orElseThrow(() -> new IllegalArgumentException("Community not found"));
        String journalId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        int fiscalYear = now.getMonthValue() >= 4 ? now.getYear() : now.getYear() - 1;
        int fiscalPeriod = now.getMonthValue() >= 4 ? now.getMonthValue() - 3 : now.getMonthValue() + 9;

        for (JournalLine line : lines) {
            LedgerAccount account = accountRepo.findById(line.accountId())
                    .orElseThrow(() -> new IllegalArgumentException("Account not found: " + line.accountId()));

            LedgerEntry entry = LedgerEntry.builder()
                    .journalId(journalId)
                    .community(community)
                    .account(account)
                    .entryType(line.entryType())
                    .amount(line.amount())
                    .referenceType(referenceType)
                    .referenceId(referenceId)
                    .narration(narration)
                    .postedAt(now)
                    .postedBy(postedBy)
                    .fiscalYear(fiscalYear)
                    .fiscalPeriod(fiscalPeriod)
                    .build();
            entryRepo.save(entry);

            if ("DEBIT".equals(line.entryType())) {
                if ("ASSET".equals(account.getAccountType()) || "EXPENSE".equals(account.getAccountType())) {
                    account.setCurrentBalance(account.getCurrentBalance().add(line.amount()));
                } else {
                    account.setCurrentBalance(account.getCurrentBalance().subtract(line.amount()));
                }
            } else {
                if ("ASSET".equals(account.getAccountType()) || "EXPENSE".equals(account.getAccountType())) {
                    account.setCurrentBalance(account.getCurrentBalance().subtract(line.amount()));
                } else {
                    account.setCurrentBalance(account.getCurrentBalance().add(line.amount()));
                }
            }
            accountRepo.save(account);
        }

        log.info("Posted journal {} with {} lines for community {}", journalId, lines.size(), communityId);
        return journalId;
    }

    @Transactional
    public void recordBillingEntry(Long communityId, Invoice invoice) {
        LedgerAccount receivable = accountRepo.findByCommunityIdAndAccountCode(communityId, "1100")
                .orElse(null);
        LedgerAccount maintenanceIncome = accountRepo.findByCommunityIdAndAccountCode(communityId, "4000")
                .orElse(null);
        LedgerAccount gstPayable = accountRepo.findByCommunityIdAndAccountCode(communityId, "2100")
                .orElse(null);

        if (receivable == null || maintenanceIncome == null) {
            log.warn("Ledger accounts not initialized for community {} — skipping ledger entry", communityId);
            return;
        }

        List<JournalLine> lines = new ArrayList<>();
        lines.add(new JournalLine(receivable.getId(), "DEBIT", invoice.getTotalAmount()));
        lines.add(new JournalLine(maintenanceIncome.getId(), "CREDIT", invoice.getTaxableAmount()));

        BigDecimal gstTotal = invoice.getCgst().add(invoice.getSgst());
        if (gstPayable != null && gstTotal.compareTo(BigDecimal.ZERO) > 0) {
            lines.add(new JournalLine(gstPayable.getId(), "CREDIT", gstTotal));
        }

        postJournalEntry(communityId, lines,
                "Maintenance invoice " + invoice.getInvoiceNumber(),
                "INVOICE", invoice.getId(), null);
    }

    @Transactional
    public void recordPaymentEntry(Long communityId, Invoice invoice, String paymentMode) {
        LedgerAccount bankAccount = accountRepo.findByCommunityIdAndAccountCode(communityId, "1000")
                .orElse(null);
        LedgerAccount receivable = accountRepo.findByCommunityIdAndAccountCode(communityId, "1100")
                .orElse(null);

        if (bankAccount == null || receivable == null) {
            log.warn("Ledger accounts not initialized for community {} — skipping payment entry", communityId);
            return;
        }

        List<JournalLine> lines = List.of(
                new JournalLine(bankAccount.getId(), "DEBIT", invoice.getTotalAmount()),
                new JournalLine(receivable.getId(), "CREDIT", invoice.getTotalAmount())
        );

        postJournalEntry(communityId, lines,
                "Payment received for " + invoice.getInvoiceNumber() + " via " + paymentMode,
                "PAYMENT", invoice.getId(), null);
    }

    @Transactional
    public String reverseJournalEntry(String journalId, Long postedBy) {
        List<LedgerEntry> entries = entryRepo.findByJournalId(journalId);
        if (entries.isEmpty()) {
            throw new IllegalArgumentException("Journal not found: " + journalId);
        }

        Long communityId = entries.get(0).getCommunity().getId();
        List<JournalLine> reversalLines = entries.stream()
                .map(e -> new JournalLine(e.getAccount().getId(),
                        "DEBIT".equals(e.getEntryType()) ? "CREDIT" : "DEBIT",
                        e.getAmount()))
                .toList();

        String reversalJournalId = postJournalEntry(communityId, reversalLines,
                "Reversal of " + journalId, "REVERSAL", null, postedBy);

        entries.forEach(e -> {
            e.setIsReversed(true);
            e.setReversalJournalId(reversalJournalId);
            entryRepo.save(e);
        });

        return reversalJournalId;
    }

    @Transactional(readOnly = true)
    public List<LedgerAccount> getChartOfAccounts(Long communityId) {
        return accountRepo.findByCommunityIdAndActiveTrue(communityId);
    }

    @Transactional(readOnly = true)
    public Page<LedgerEntry> getJournalEntries(Long communityId, int page, int size) {
        return entryRepo.findByCommunityIdOrderByPostedAtDesc(communityId, PageRequest.of(page, size));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getTrialBalance(Long communityId, int fiscalYear) {
        List<LedgerAccount> accounts = accountRepo.findByCommunityIdAndActiveTrue(communityId);
        List<Map<String, Object>> rows = new ArrayList<>();
        BigDecimal totalDebits = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;

        for (LedgerAccount acct : accounts) {
            BigDecimal balance = acct.getCurrentBalance();
            if (balance.compareTo(BigDecimal.ZERO) == 0) continue;

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("accountCode", acct.getAccountCode());
            row.put("accountName", acct.getAccountName());
            row.put("accountType", acct.getAccountType());

            if ("ASSET".equals(acct.getAccountType()) || "EXPENSE".equals(acct.getAccountType())) {
                if (balance.compareTo(BigDecimal.ZERO) > 0) {
                    row.put("debit", balance);
                    row.put("credit", BigDecimal.ZERO);
                    totalDebits = totalDebits.add(balance);
                } else {
                    row.put("debit", BigDecimal.ZERO);
                    row.put("credit", balance.abs());
                    totalCredits = totalCredits.add(balance.abs());
                }
            } else {
                if (balance.compareTo(BigDecimal.ZERO) > 0) {
                    row.put("debit", BigDecimal.ZERO);
                    row.put("credit", balance);
                    totalCredits = totalCredits.add(balance);
                } else {
                    row.put("debit", balance.abs());
                    row.put("credit", BigDecimal.ZERO);
                    totalDebits = totalDebits.add(balance.abs());
                }
            }
            rows.add(row);
        }

        return Map.of(
                "accounts", rows,
                "totalDebits", totalDebits,
                "totalCredits", totalCredits,
                "balanced", totalDebits.compareTo(totalCredits) == 0,
                "fiscalYear", fiscalYear
        );
    }

    public record JournalLine(Long accountId, String entryType, BigDecimal amount) {}
}

package com.manacommunity.api.cfbos.accounting.engine;

import com.manacommunity.api.cfbos.accounting.dto.JournalEntryRequest;
import com.manacommunity.api.cfbos.accounting.entity.*;
import com.manacommunity.api.cfbos.accounting.enums.JournalEntryStatus;
import com.manacommunity.api.cfbos.accounting.enums.JournalEntryType;
import com.manacommunity.api.cfbos.accounting.repository.*;
import com.manacommunity.api.cfbos.shared.enums.DocumentType;
import com.manacommunity.api.cfbos.shared.exception.CfbosException;
import com.manacommunity.api.cfbos.shared.exception.CfbosResourceNotFoundException;
import com.manacommunity.api.cfbos.shared.exception.UnbalancedJournalEntryException;
import com.manacommunity.api.cfbos.shared.sequence.service.DocumentSequenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountingEngine {

    private final JournalEntryRepository journalEntryRepository;
    private final JournalLineRepository journalLineRepository;
    private final AccountRepository accountRepository;
    private final FiscalYearRepository fiscalYearRepository;
    private final AccountingPeriodRepository accountingPeriodRepository;
    private final DocumentSequenceService documentSequenceService;

    @Transactional
    public JournalEntry createAndPostJournalEntry(JournalEntryRequest request) {
        FiscalYear fy = fiscalYearRepository.findByIsCurrentTrue()
                .orElseThrow(() -> new CfbosException("No active fiscal year found"));

        validateBalance(request.getLines());

        AccountingPeriod period = accountingPeriodRepository
                .findByFiscalYearAndDate(fy, request.getEntryDate())
                .orElseThrow(() -> new CfbosException(
                        "No accounting period found for date: " + request.getEntryDate()));

        if ("CLOSED".equals(period.getStatus())) {
            throw new CfbosException("Accounting period is closed: " + period.getName());
        }

        String entryNumber = documentSequenceService.nextNumber(
                DocumentType.JOURNAL_ENTRY, fy.getName());

        JournalEntry entry = JournalEntry.builder()
                .entryNumber(entryNumber)
                .entryDate(request.getEntryDate())
                .fiscalYear(fy)
                .accountingPeriod(period)
                .entryType(JournalEntryType.STANDARD)
                .sourceModule(request.getSourceModule())
                .sourceDocumentType(request.getSourceDocumentType())
                .sourceDocumentId(request.getSourceDocumentId())
                .narration(request.getNarration())
                .totalDebit(BigDecimal.ZERO)
                .totalCredit(BigDecimal.ZERO)
                .status(JournalEntryStatus.POSTED)
                .postedAt(LocalDateTime.now())
                .lines(new ArrayList<>())
                .build();

        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;

        for (JournalEntryRequest.LineRequest lineReq : request.getLines()) {
            Account account = accountRepository.findByCode(lineReq.getAccountCode())
                    .orElseThrow(() -> new CfbosResourceNotFoundException(
                            "Account " + lineReq.getAccountCode(), 0L));

            JournalLine line = JournalLine.builder()
                    .journalEntry(entry)
                    .account(account)
                    .debitAmount(lineReq.getDebitAmount() != null ? lineReq.getDebitAmount() : BigDecimal.ZERO)
                    .creditAmount(lineReq.getCreditAmount() != null ? lineReq.getCreditAmount() : BigDecimal.ZERO)
                    .narration(lineReq.getNarration())
                    .costCenterId(lineReq.getCostCenterId())
                    .fundId(lineReq.getFundId())
                    .build();

            entry.getLines().add(line);
            totalDebit = totalDebit.add(line.getDebitAmount());
            totalCredit = totalCredit.add(line.getCreditAmount());

            updateAccountBalance(account, line.getDebitAmount(), line.getCreditAmount());
        }

        entry.setTotalDebit(totalDebit);
        entry.setTotalCredit(totalCredit);

        return journalEntryRepository.save(entry);
    }

    @Transactional
    public JournalEntry postJournalEntry(Long journalEntryId) {
        JournalEntry entry = journalEntryRepository.findById(journalEntryId)
                .orElseThrow(() -> new CfbosResourceNotFoundException("JournalEntry", journalEntryId));

        if (entry.getStatus() != JournalEntryStatus.DRAFT) {
            throw new CfbosException("Only DRAFT journal entries can be posted");
        }

        entry.setStatus(JournalEntryStatus.POSTED);
        entry.setPostedAt(LocalDateTime.now());

        return journalEntryRepository.save(entry);
    }

    @Transactional
    public JournalEntry reverseJournalEntry(Long journalEntryId, String reason) {
        JournalEntry original = journalEntryRepository.findById(journalEntryId)
                .orElseThrow(() -> new CfbosResourceNotFoundException("JournalEntry", journalEntryId));

        if (original.getStatus() != JournalEntryStatus.POSTED) {
            throw new CfbosException("Can only reverse POSTED journal entries");
        }

        List<JournalEntryRequest.LineRequest> reversedLines = original.getLines().stream()
                .map(line -> JournalEntryRequest.LineRequest.builder()
                        .accountCode(line.getAccount().getCode())
                        .debitAmount(line.getCreditAmount())
                        .creditAmount(line.getDebitAmount())
                        .narration("Reversal: " + (line.getNarration() != null ? line.getNarration() : ""))
                        .build())
                .toList();

        JournalEntryRequest reversalRequest = JournalEntryRequest.builder()
                .entryDate(original.getEntryDate())
                .narration("Reversal of " + original.getEntryNumber() + ": " + reason)
                .sourceModule(original.getSourceModule())
                .sourceDocumentType(original.getSourceDocumentType())
                .sourceDocumentId(original.getSourceDocumentId())
                .lines(reversedLines)
                .build();

        JournalEntry reversal = createAndPostJournalEntry(reversalRequest);
        reversal.setEntryType(JournalEntryType.REVERSING);
        reversal.setReversalOfId(original.getId());

        original.setStatus(JournalEntryStatus.REVERSED);
        original.setReversedBy(null);
        original.setReversedAt(LocalDateTime.now());
        journalEntryRepository.save(original);

        return journalEntryRepository.save(reversal);
    }

    private void validateBalance(List<JournalEntryRequest.LineRequest> lines) {
        BigDecimal totalDebit = lines.stream()
                .map(l -> l.getDebitAmount() != null ? l.getDebitAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCredit = lines.stream()
                .map(l -> l.getCreditAmount() != null ? l.getCreditAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalDebit.compareTo(totalCredit) != 0) {
            throw new UnbalancedJournalEntryException(totalDebit, totalCredit);
        }
    }

    private void updateAccountBalance(Account account, BigDecimal debit, BigDecimal credit) {
        switch (account.getAccountType()) {
            case ASSET, EXPENSE -> account.setCurrentBalance(
                    account.getCurrentBalance().add(debit).subtract(credit));
            case LIABILITY, EQUITY, INCOME -> account.setCurrentBalance(
                    account.getCurrentBalance().add(credit).subtract(debit));
        }
    }
}

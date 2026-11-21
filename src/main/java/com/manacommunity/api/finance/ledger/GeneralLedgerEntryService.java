package com.manacommunity.api.finance.ledger;

import com.manacommunity.api.finance.entity.GeneralLedgerEntry;
import com.manacommunity.api.finance.repository.GeneralLedgerEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * Posts balanced double-entry rows to the general_ledger_entry table.
 * Each call writes exactly two rows — a debit row and a credit row — in the
 * same transaction.  Throws {@link IllegalArgumentException} if the amount is
 * not positive.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GeneralLedgerEntryService {

    /** Static names for the standard account codes used by billing integration. */
    private static final Map<String, String> ACCOUNT_NAMES = Map.of(
            "1010", "Cash / Bank",
            "1100", "Accounts Receivable",
            "3100", "Maintenance Income"
    );

    private final GeneralLedgerEntryRepository ledgerRepo;

    /**
     * Post a balanced double-entry pair.
     *
     * @param communityId       the community this entry belongs to (required)
     * @param debitAccountCode  account code that receives the debit
     * @param creditAccountCode account code that receives the credit
     * @param amount            positive transaction amount
     * @param description       human-readable narrative
     * @param referenceType     source document type, e.g. "INVOICE" or "PAYMENT"
     * @param referenceId       source document identifier, e.g. the invoice id or code
     * @param entryDate         accounting date
     */
    @Transactional
    public void postDoubleEntry(Long communityId,
                                String debitAccountCode,
                                String creditAccountCode,
                                BigDecimal amount,
                                String description,
                                String referenceType,
                                String referenceId,
                                LocalDate entryDate) {

        if (communityId == null) {
            log.warn("Skipping ledger entry — communityId is null for reference {}/{}", referenceType, referenceId);
            return;
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Ledger entry amount must be positive, got: " + amount);
        }

        String safeRefId = referenceId != null && referenceId.length() > 30
                ? referenceId.substring(0, 30) : referenceId;

        GeneralLedgerEntry debitRow = GeneralLedgerEntry.builder()
                .communityId(communityId)
                .entryDate(entryDate)
                .accountCode(debitAccountCode)
                .accountName(ACCOUNT_NAMES.getOrDefault(debitAccountCode, debitAccountCode))
                .debit(amount)
                .credit(BigDecimal.ZERO)
                .description(description)
                .referenceType(referenceType)
                .referenceId(safeRefId)
                .build();

        GeneralLedgerEntry creditRow = GeneralLedgerEntry.builder()
                .communityId(communityId)
                .entryDate(entryDate)
                .accountCode(creditAccountCode)
                .accountName(ACCOUNT_NAMES.getOrDefault(creditAccountCode, creditAccountCode))
                .debit(BigDecimal.ZERO)
                .credit(amount)
                .description(description)
                .referenceType(referenceType)
                .referenceId(safeRefId)
                .build();

        ledgerRepo.save(debitRow);
        ledgerRepo.save(creditRow);

        log.debug("Ledger posted: community={} DR {} CR {} amount={} ref={}/{}",
                communityId, debitAccountCode, creditAccountCode, amount, referenceType, safeRefId);
    }
}

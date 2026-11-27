package com.manacommunity.api.finance.ledger;

import com.manacommunity.api.finance.entity.GeneralLedgerEntry;
import com.manacommunity.api.finance.repository.GeneralLedgerEntryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * Posts balanced double-entry rows to the {@code general_ledger_entry} table.
 * Each successful call writes exactly two rows — a debit row and a credit row — in one
 * transaction.
 *
 * <h3>Idempotency</h3>
 * A source document is posted at most once. {@link #postDoubleEntry} first checks whether
 * the {@code (communityId, referenceType, referenceId)} triple has already been posted and
 * returns without writing if so; the unique index added in V504 is the database-level
 * backstop for the concurrent case, and a violation of it is treated as "already posted"
 * rather than surfacing as a 500.
 */
@Slf4j
@Service
public class GeneralLedgerEntryService {

    /** Static names for the standard account codes used by billing integration. */
    private static final Map<String, String> ACCOUNT_NAMES = Map.of(
            "1010", "Cash / Bank",
            "1100", "Accounts Receivable",
            "3100", "Maintenance Income"
    );

    /** Matches the widened reference_id column (V504). */
    private static final int MAX_REFERENCE_ID_LENGTH = 100;

    private final GeneralLedgerEntryRepository ledgerRepo;

    /**
     * Self-reference so {@link #writeDoubleEntry} is invoked through the Spring proxy and
     * really runs in its own {@code REQUIRES_NEW} transaction. That keeps a unique-index
     * violation from poisoning the caller's transaction, which is what lets the violation
     * be swallowed as an idempotent no-op instead of becoming an
     * {@code UnexpectedRollbackException}.
     */
    private final ObjectProvider<GeneralLedgerEntryService> self;

    public GeneralLedgerEntryService(GeneralLedgerEntryRepository ledgerRepo,
                                     ObjectProvider<GeneralLedgerEntryService> self) {
        this.ledgerRepo = ledgerRepo;
        // ObjectProvider is a factory handle: nothing is resolved during context startup,
        // so this self-reference cannot introduce a circular-dependency failure at boot.
        this.self = self;
    }

    /**
     * Post a balanced double-entry pair, at most once per source document.
     *
     * @param communityId       the community this entry belongs to (required)
     * @param debitAccountCode  account code that receives the debit
     * @param creditAccountCode account code that receives the credit
     * @param amount            positive transaction amount
     * @param description       human-readable narrative
     * @param referenceType     source document type, e.g. "INVOICE" or "PAYMENT"
     * @param referenceId       source document identifier, e.g. the invoice id or code
     * @param entryDate         accounting date
     * @throws IllegalStateException    if {@code communityId} is null
     * @throws IllegalArgumentException if {@code amount} is not positive
     */
    public void postDoubleEntry(Long communityId,
                                String debitAccountCode,
                                String creditAccountCode,
                                BigDecimal amount,
                                String description,
                                String referenceType,
                                String referenceId,
                                LocalDate entryDate) {

        if (communityId == null) {
            // Previously logged a warning and returned, which left the invoice with no
            // journal entry at all and nothing to recover from. A missing journal entry is
            // worse than a failed request, so throw. This runs on an @Async thread, where
            // the exception only reaches the uncaught handler, hence the ERROR log with the
            // reference so the posting can be replayed by hand.
            log.error("Refusing to post ledger entry without a community: ref={}/{} amount={} desc={}",
                    referenceType, referenceId, amount, description);
            throw new IllegalStateException("Cannot post a ledger entry without a communityId "
                    + "(reference " + referenceType + "/" + referenceId + ")");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Ledger entry amount must be positive, got: " + amount);
        }
        if (debitAccountCode != null && debitAccountCode.equals(creditAccountCode)) {
            // Not a double entry, and the two rows would also collide on the V504 index.
            throw new IllegalArgumentException(
                    "Debit and credit account codes must differ, both were: " + debitAccountCode);
        }

        // Truncation only guards against overflowing the column; at 100 characters it no
        // longer collapses distinct references the way the old 30-character cut did.
        String safeRefId = referenceId != null && referenceId.length() > MAX_REFERENCE_ID_LENGTH
                ? referenceId.substring(0, MAX_REFERENCE_ID_LENGTH) : referenceId;

        if (ledgerRepo.existsByCommunityIdAndReferenceTypeAndReferenceId(
                communityId, referenceType, safeRefId)) {
            log.info("Ledger entry for {}/{} in community {} already exists - skipping duplicate posting",
                    referenceType, safeRefId, communityId);
            return;
        }

        try {
            self.getObject().writeDoubleEntry(communityId, debitAccountCode, creditAccountCode,
                    amount, description, referenceType, safeRefId, entryDate);
        } catch (DataIntegrityViolationException alreadyPosted) {
            // The V504 unique index fired: a concurrent caller posted the same pair between
            // the check above and the insert. The books are already correct, so this is an
            // idempotent no-op, not a failure.
            log.warn("Ledger entry for {}/{} in community {} was posted concurrently - "
                            + "treating as an idempotent no-op",
                    referenceType, safeRefId, communityId);
        }
    }

    /**
     * Writes the debit/credit pair in its own transaction. Public only so the Spring proxy
     * can intercept it — call {@link #postDoubleEntry} instead.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void writeDoubleEntry(Long communityId,
                                 String debitAccountCode,
                                 String creditAccountCode,
                                 BigDecimal amount,
                                 String description,
                                 String referenceType,
                                 String referenceId,
                                 LocalDate entryDate) {

        GeneralLedgerEntry debitRow = GeneralLedgerEntry.builder()
                .communityId(communityId)
                .entryDate(entryDate)
                .accountCode(debitAccountCode)
                .accountName(ACCOUNT_NAMES.getOrDefault(debitAccountCode, debitAccountCode))
                .debit(amount)
                .credit(BigDecimal.ZERO)
                .description(description)
                .referenceType(referenceType)
                .referenceId(referenceId)
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
                .referenceId(referenceId)
                .build();

        ledgerRepo.save(debitRow);
        ledgerRepo.save(creditRow);
        ledgerRepo.flush();   // surface a unique-index violation here, inside this transaction

        log.debug("Ledger posted: community={} DR {} CR {} amount={} ref={}/{}",
                communityId, debitAccountCode, creditAccountCode, amount, referenceType, referenceId);
    }
}

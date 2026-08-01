package com.manacommunity.api.cfbos.shared.exception;

import org.springframework.http.HttpStatus;
import java.math.BigDecimal;

public class UnbalancedJournalEntryException extends CfbosException {
    public UnbalancedJournalEntryException(BigDecimal totalDebit, BigDecimal totalCredit) {
        super("Journal entry is not balanced. Debit: " + totalDebit + ", Credit: " + totalCredit,
              HttpStatus.BAD_REQUEST, "CFBOS_UNBALANCED_ENTRY");
    }
}

package com.manacommunity.api.cfbos.treasury.parser;

import com.manacommunity.api.cfbos.treasury.entity.BankStatementTransaction;
import com.manacommunity.api.cfbos.treasury.enums.ReconciliationStatus;
import com.manacommunity.api.cfbos.treasury.enums.StatementType;
import com.manacommunity.api.model.Community;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class Mt940BankStatementParser {

    private static final Pattern TAG_61_PATTERN = Pattern.compile(":61:(\\d{6})(\\d{4})?(C|D|RC|RD)([A-Z]?)(\\d+,\\d{2}|\\d+\\.\\d{2})([A-Z0-9]+)?//?([A-Z0-9]+)?");
    private static final Pattern FLAT_PATTERN = Pattern.compile("(?i)\\b([A-Z]\\d?[-/ ]?\\d{2,4})\\b");

    public List<BankStatementTransaction> parse(String content, Community community) {
        List<BankStatementTransaction> transactions = new ArrayList<>();
        String[] lines = content.split("\\r?\\n");

        BankStatementTransaction currentTx = null;
        StringBuilder narrationBuilder = new StringBuilder();

        for (String line : lines) {
            line = line.trim();
            if (line.startsWith(":61:")) {
                if (currentTx != null) {
                    finalizeTransaction(currentTx, narrationBuilder.toString());
                    transactions.add(currentTx);
                    narrationBuilder.setLength(0);
                }

                currentTx = new BankStatementTransaction();
                currentTx.setCommunity(community);
                currentTx.setStatementType(StatementType.SWIFT_MT940);
                currentTx.setStatus(ReconciliationStatus.UNMATCHED);

                parseTag61(line, currentTx);
            } else if (line.startsWith(":86:")) {
                narrationBuilder.append(line.substring(4)).append(" ");
            } else if (currentTx != null && !line.startsWith(":")) {
                narrationBuilder.append(line).append(" ");
            }
        }

        if (currentTx != null) {
            finalizeTransaction(currentTx, narrationBuilder.toString());
            transactions.add(currentTx);
        }

        return transactions;
    }

    private void parseTag61(String line, BankStatementTransaction tx) {
        try {
            Matcher m = TAG_61_PATTERN.matcher(line);
            if (m.find()) {
                String dateStr = m.group(1);
                String crDr = m.group(3);
                String amtStr = m.group(5).replace(",", ".");
                String ref = m.group(7) != null ? m.group(7) : (m.group(6) != null ? m.group(6) : "REF");

                int year = 2000 + Integer.parseInt(dateStr.substring(0, 2));
                int month = Integer.parseInt(dateStr.substring(2, 4));
                int day = Integer.parseInt(dateStr.substring(4, 6));

                tx.setTransactionDate(LocalDate.of(year, month, day));
                tx.setEntryType(crDr.contains("C") ? "CR" : "DR");
                tx.setAmount(new BigDecimal(amtStr));
                tx.setReferenceNumber(ref);
            } else {
                tx.setTransactionDate(LocalDate.now());
                tx.setEntryType("CR");
                tx.setAmount(BigDecimal.ZERO);
                tx.setReferenceNumber("MT940-TX");
            }
        } catch (Exception e) {
            tx.setTransactionDate(LocalDate.now());
            tx.setEntryType("CR");
            tx.setAmount(BigDecimal.ZERO);
            tx.setReferenceNumber("MT940-ERR");
        }
    }

    private void finalizeTransaction(BankStatementTransaction tx, String narration) {
        tx.setNarration(narration.trim());
        Matcher flatMatcher = FLAT_PATTERN.matcher(narration);
        if (flatMatcher.find()) {
            tx.setDetectedFlatNumber(flatMatcher.group(1).toUpperCase().replace(" ", ""));
        }
    }
}

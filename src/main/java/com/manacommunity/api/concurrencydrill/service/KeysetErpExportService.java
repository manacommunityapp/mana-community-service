package com.manacommunity.api.concurrencydrill.service;

import com.manacommunity.api.concurrencydrill.repository.AtomicErpReceiptRepository;
import com.manacommunity.api.concurrencydrill.repository.AtomicErpReceiptRepository.ReceiptExportRow;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.Writer;
import java.time.Instant;
import java.util.List;

/**
 * Memory-Bounded Keyset ERP Streaming Service:
 * Replaces N+1 per-flat queries and full-heap PDF/CSV buffering with constant O(B) memory
 * keyset cursor batching (id > cursorId) streamed straight to the HTTP response writer.
 */
@Service
@RequiredArgsConstructor
public class KeysetErpExportService {

    public static final int DEFAULT_BATCH_SIZE = 500;
    private final AtomicErpReceiptRepository receiptRepo;

    @Transactional(readOnly = true)
    public long streamReceiptsCsv(
            Long communityId,
            Instant fromTime,
            Instant toTime,
            Writer writer,
            int requestedBatchSize
    ) throws IOException {
        if (communityId == null || fromTime == null || toTime == null || toTime.isBefore(fromTime)) {
            throw new IllegalArgumentException("Valid communityId and [fromTime, toTime] range required");
        }
        int batchSize = Math.max(1, Math.min(requestedBatchSize > 0 ? requestedBatchSize : DEFAULT_BATCH_SIZE, 1000));

        writer.write("receipt_id,flat_no,amount_paise,payment_mode,paid_at\n");
        long cursorId = 0L;
        long totalExported = 0L;

        while (true) {
            List<ReceiptExportRow> batch = receiptRepo.fetchExportBatch(
                    communityId, fromTime, toTime, cursorId, PageRequest.of(0, batchSize));
            if (batch == null || batch.isEmpty()) {
                break;
            }
            for (ReceiptExportRow row : batch) {
                writer.write(row.receiptId() + ","
                        + sanitizeCsvCell(row.flatNo()) + ","
                        + row.amountPaise() + ","
                        + sanitizeCsvCell(row.paymentMode()) + ","
                        + row.paidAt() + "\n");
                cursorId = row.receiptId();
                totalExported++;
            }
            writer.flush();
            if (batch.size() < batchSize) {
                break;
            }
        }
        return totalExported;
    }

    /**
     * Neutralizes CSV formula injection (=, +, -, @) and properly quotes fields with commas/quotes.
     */
    public static String sanitizeCsvCell(String value) {
        if (value == null) return "";
        String trimmed = value.replace("\r", " ").replace("\n", " ");
        if (!trimmed.isEmpty()) {
            char first = trimmed.charAt(0);
            if (first == '=' || first == '+' || first == '-' || first == '@') {
                trimmed = "'" + trimmed;
            }
        }
        if (trimmed.contains(",") || trimmed.contains("\"")) {
            return "\"" + trimmed.replace("\"", "\"\"") + "\"";
        }
        return trimmed;
    }
}

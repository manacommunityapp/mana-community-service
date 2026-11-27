package com.manacommunity.api.db.optimization.mysql;

import com.manacommunity.api.db.optimization.mysql.model.KeysetCursorPage;
import com.manacommunity.api.db.optimization.mysql.model.VisitorCursorRecord;
import com.manacommunity.api.db.optimization.mysql.service.KeysetPaginationEngine;
import com.manacommunity.api.db.optimization.mysql.service.KeysetPaginationEngineImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class KeysetPaginationEngineTest {

    private KeysetPaginationEngine paginationEngine;
    private static final Long COMMUNITY_ID = 505L;

    @BeforeEach
    void setUp() {
        paginationEngine = new KeysetPaginationEngineImpl();
        List<VisitorCursorRecord> records = new ArrayList<>();
        Instant baseTime = Instant.parse("2026-11-20T10:00:00Z");

        // Seed 100 visitor records with varying timestamps and IDs
        for (long i = 1; i <= 100; i++) {
            records.add(VisitorCursorRecord.builder()
                    .id(i)
                    .communityId(COMMUNITY_ID)
                    .passCode("PASS-" + (1000 + i))
                    .visitorName("Visitor-" + i)
                    .vehicleNumber("KA01AB" + (1000 + i))
                    .status("CHECKED_IN")
                    .createdAt(baseTime.plus(i * 10, ChronoUnit.MINUTES))
                    .build());
        }
        paginationEngine.seedRecords(records);
    }

    @Test
    @DisplayName("Keyset Pagination sequentially streams records in strict descending order with zero duplicates")
    void testSequentialKeysetPagination() {
        int pageSize = 25;
        String cursor = null;
        Set<Long> visitedIds = new HashSet<>();
        int pageCount = 0;

        while (true) {
            KeysetCursorPage<VisitorCursorRecord> page = paginationEngine.queryVisitorPasses(COMMUNITY_ID, cursor, pageSize);
            assertNotNull(page);
            pageCount++;

            List<VisitorCursorRecord> items = page.getItems();
            assertFalse(items.isEmpty());
            assertEquals(25, items.size());

            // Verify strict descending order
            for (int i = 0; i < items.size() - 1; i++) {
                VisitorCursorRecord current = items.get(i);
                VisitorCursorRecord next = items.get(i + 1);
                assertTrue(current.getCreatedAt().compareTo(next.getCreatedAt()) >= 0);
            }

            // Verify no duplicate IDs across pages
            for (VisitorCursorRecord rec : items) {
                boolean isNew = visitedIds.add(rec.getId());
                assertTrue(isNew, "Encountered duplicate record ID: " + rec.getId());
            }

            cursor = page.getNextCursor();
            if (!page.isHasNext()) {
                break;
            }
        }

        assertEquals(4, pageCount, "100 records with pageSize 25 should yield exactly 4 pages");
        assertEquals(100, visitedIds.size(), "Should have traversed all 100 records");
    }

    @Test
    @DisplayName("Cursor encoding and decoding maintains precision for high-frequency millisecond timestamps")
    void testCursorEncodeDecode() {
        Instant now = Instant.now();
        VisitorCursorRecord record = VisitorCursorRecord.builder()
                .id(987654L)
                .communityId(COMMUNITY_ID)
                .createdAt(now)
                .build();

        String cursor = paginationEngine.encodeCursor(record);
        assertNotNull(cursor);

        long[] decoded = paginationEngine.decodeCursor(cursor);
        assertEquals(now.toEpochMilli(), decoded[0]);
        assertEquals(987654L, decoded[1]);
    }
}

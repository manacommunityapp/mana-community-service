package com.manacommunity.api.db.optimization.mysql.service;

import com.manacommunity.api.db.optimization.mysql.model.KeysetCursorPage;
import com.manacommunity.api.db.optimization.mysql.model.VisitorCursorRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
public class KeysetPaginationEngineImpl implements KeysetPaginationEngine {

    // Simulates an indexed B-Tree sorted by (community_id ASC, created_at DESC, id DESC)
    private final Map<Long, NavigableSet<VisitorCursorRecord>> communityIndexes = new ConcurrentHashMap<>();

    private static final Comparator<VisitorCursorRecord> COMPOSITE_CURSOR_COMPARATOR = (a, b) -> {
        // Primary sort: created_at DESC (latest first)
        int timeCmp = b.getCreatedAt().compareTo(a.getCreatedAt());
        if (timeCmp != 0) {
            return timeCmp;
        }
        // Secondary tie-breaker: id DESC (largest first)
        return b.getId().compareTo(a.getId());
    };

    @Override
    public void seedRecords(List<VisitorCursorRecord> records) {
        for (VisitorCursorRecord rec : records) {
            communityIndexes.computeIfAbsent(rec.getCommunityId(), k -> new TreeSet<>(COMPOSITE_CURSOR_COMPARATOR))
                            .add(rec);
        }
    }

    @Override
    public KeysetCursorPage<VisitorCursorRecord> queryVisitorPasses(Long communityId, String cursor, int pageSize) {
        NavigableSet<VisitorCursorRecord> index = communityIndexes.get(communityId);
        if (index == null || index.isEmpty() || pageSize <= 0) {
            return KeysetCursorPage.<VisitorCursorRecord>builder()
                    .items(Collections.emptyList())
                    .hasNext(false)
                    .pageSize(pageSize)
                    .build();
        }

        NavigableSet<VisitorCursorRecord> searchView;
        if (cursor == null || cursor.isBlank()) {
            // First page: start from the very beginning of the index
            searchView = index;
        } else {
            // Seek directly to the keyset position using (createdAt, id)
            long[] decoded = decodeCursor(cursor);
            long cursorEpochMs = decoded[0];
            long cursorId = decoded[1];

            VisitorCursorRecord probe = VisitorCursorRecord.builder()
                    .id(cursorId)
                    .communityId(communityId)
                    .createdAt(Instant.ofEpochMilli(cursorEpochMs))
                    .build();

            // tailSet(probe, false) performs an O(log N) B-Tree seek directly past the cursor
            searchView = index.tailSet(probe, false);
        }

        // Fetch pageSize + 1 to determine hasNext without an expensive COUNT(*) query
        List<VisitorCursorRecord> results = new ArrayList<>();
        Iterator<VisitorCursorRecord> it = searchView.iterator();
        while (it.hasNext() && results.size() <= pageSize) {
            results.add(it.next());
        }

        boolean hasNext = results.size() > pageSize;
        if (hasNext) {
            results.remove(results.size() - 1);
        }

        String nextCursor = null;
        if (hasNext && !results.isEmpty()) {
            VisitorCursorRecord lastRecord = results.get(results.size() - 1);
            nextCursor = encodeCursor(lastRecord);
        }

        return KeysetCursorPage.<VisitorCursorRecord>builder()
                .items(results)
                .nextCursor(nextCursor)
                .hasNext(hasNext)
                .pageSize(pageSize)
                .build();
    }

    @Override
    public String encodeCursor(VisitorCursorRecord record) {
        if (record == null || record.getCreatedAt() == null || record.getId() == null) {
            return null;
        }
        String raw = record.getCreatedAt().toEpochMilli() + "_" + record.getId();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public long[] decodeCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            throw new IllegalArgumentException("Cursor cannot be null or empty");
        }
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(cursor);
            String raw = new String(decoded, StandardCharsets.UTF_8);
            String[] parts = raw.split("_");
            if (parts.length != 2) {
                throw new IllegalArgumentException("Malformed cursor string: " + raw);
            }
            return new long[]{Long.parseLong(parts[0]), Long.parseLong(parts[1])};
        } catch (Exception ex) {
            log.error("Failed to decode keyset cursor '{}'", cursor, ex);
            throw new IllegalArgumentException("Invalid cursor format", ex);
        }
    }
}

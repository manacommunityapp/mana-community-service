package com.manacommunity.api.db.optimization.sharding;

import com.manacommunity.api.db.optimization.sharding.model.ShardedVisitorEventDto;
import com.manacommunity.api.db.optimization.sharding.service.HotPartitionScatterGatherEngineImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HotPartitionScatterGatherEngineTest {

    private HotPartitionScatterGatherEngineImpl engine;
    private final Long communityId = 7001L;

    @BeforeEach
    void setUp() {
        engine = new HotPartitionScatterGatherEngineImpl();
        engine.clear();
    }

    @Test
    @DisplayName("Should distribute 50x event spike uniformly across 16 shards and stream globally sorted range queries")
    void testHotPartitionWriteShardingAndScatterGather() {
        LocalDateTime baseTime = LocalDateTime.of(2026, 11, 27, 18, 0, 0);

        // Write 160 visitor events across different flats and gates over a 60-minute window
        for (int i = 0; i < 160; i++) {
            LocalDateTime eventTime = baseTime.plusSeconds(i * 20); // incrementing timestamp
            engine.writeEvent(
                    communityId,
                    "Visitor-" + (i % 25),
                    "Flat-" + (100 + (i % 40)),
                    "Gate-" + (1 + (i % 3)),
                    eventTime);
        }

        // 1. Verify Write-Sharding Distribution
        Map<Integer, Long> distribution = engine.getShardDistribution(communityId);
        assertEquals(16, distribution.size(), "Must utilize all 16 physical storage shards");

        for (Map.Entry<Integer, Long> entry : distribution.entrySet()) {
            assertTrue(entry.getValue() > 0, "Shard " + entry.getKey() + " must have received writes");
        }

        // 2. Query Range across all 16 shards using Min-Heap k-Way Merge
        LocalDateTime queryStart = baseTime.plusMinutes(10);
        LocalDateTime queryEnd = baseTime.plusMinutes(40);

        List<ShardedVisitorEventDto> results = engine.queryRangeSorted(communityId, queryStart, queryEnd);

        assertNotNull(results);
        assertFalse(results.isEmpty());

        // 3. Verify strict monotonic timestamp ordering of merged stream
        for (int i = 0; i < results.size() - 1; i++) {
            LocalDateTime curr = results.get(i).getTimestamp();
            LocalDateTime next = results.get(i + 1).getTimestamp();
            assertFalse(next.isBefore(curr),
                    "Records must be globally sorted by timestamp: " + curr + " should be <= " + next);
            assertTrue(!curr.isBefore(queryStart) && !curr.isAfter(queryEnd),
                    "Record timestamp must be within query window");
        }
    }
}

package com.manacommunity.api.db.optimization.sharding.service;

import com.manacommunity.api.db.optimization.sharding.model.ShardedVisitorEventDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Slf4j
@Service
public class HotPartitionScatterGatherEngineImpl implements HotPartitionScatterGatherEngine {

    private final AtomicLong eventIdSeq = new AtomicLong(1000);
    // Simulates DynamoDB table: PartitionKey -> SortedList<ShardedVisitorEventDto>
    private final Map<String, List<ShardedVisitorEventDto>> tableStorage = new ConcurrentHashMap<>();

    @Override
    public ShardedVisitorEventDto writeEvent(
            Long communityId,
            String visitorName,
            String flatNumber,
            String entryGate,
            LocalDateTime timestamp) {

        String eventId = "EVT-" + eventIdSeq.incrementAndGet();

        // Calculate uniform hash modulo across 16 shards
        int hash = Objects.hash(visitorName, flatNumber, timestamp, eventId);
        int shardId = Math.abs(hash) % DEFAULT_SHARD_COUNT;

        String partitionKey = "COMMUNITY_" + communityId + "#shard_" + String.format("%02d", shardId);
        String sortKey = timestamp.toString() + "#" + eventId;

        ShardedVisitorEventDto event = ShardedVisitorEventDto.builder()
                .eventId(eventId)
                .communityId(communityId)
                .shardId(shardId)
                .partitionKey(partitionKey)
                .sortKey(sortKey)
                .timestamp(timestamp)
                .visitorName(visitorName)
                .flatNumber(flatNumber)
                .entryGate(entryGate)
                .build();

        tableStorage
                .computeIfAbsent(partitionKey, k -> new CopyOnWriteArrayList<>())
                .add(event);

        return event;
    }

    @Override
    public List<ShardedVisitorEventDto> queryRangeSorted(
            Long communityId,
            LocalDateTime startTime,
            LocalDateTime endTime) {

        // Step 1: Parallel Scatter - Issue queries across all 16 shards
        List<List<ShardedVisitorEventDto>> shardResults = new ArrayList<>();

        for (int i = 0; i < DEFAULT_SHARD_COUNT; i++) {
            String pk = "COMMUNITY_" + communityId + "#shard_" + String.format("%02d", i);
            List<ShardedVisitorEventDto> shardEvents = tableStorage.getOrDefault(pk, Collections.emptyList());

            // Filter in-range items within this shard and ensure sorted by timestamp
            List<ShardedVisitorEventDto> filtered = shardEvents.stream()
                    .filter(e -> !e.getTimestamp().isBefore(startTime) && !e.getTimestamp().isAfter(endTime))
                    .sorted(Comparator.comparing(ShardedVisitorEventDto::getTimestamp))
                    .collect(Collectors.toList());

            if (!filtered.isEmpty()) {
                shardResults.add(filtered);
            }
        }

        // Step 2: Min-Heap k-Way Merge Gather (O(M log k) complexity)
        List<ShardedVisitorEventDto> mergedSortedList = new ArrayList<>();

        // Helper class to track pointer in each shard's list
        class ShardCursor {
            final List<ShardedVisitorEventDto> list;
            int index = 0;

            ShardCursor(List<ShardedVisitorEventDto> list) {
                this.list = list;
            }

            boolean hasNext() {
                return index < list.size();
            }

            ShardedVisitorEventDto peek() {
                return list.get(index);
            }

            ShardedVisitorEventDto pop() {
                return list.get(index++);
            }
        }

        PriorityQueue<ShardCursor> minHeap = new PriorityQueue<>(
                Comparator.comparing(c -> c.peek().getTimestamp())
        );

        for (List<ShardedVisitorEventDto> shardList : shardResults) {
            minHeap.offer(new ShardCursor(shardList));
        }

        while (!minHeap.isEmpty()) {
            ShardCursor cursor = minHeap.poll();
            mergedSortedList.add(cursor.pop());

            if (cursor.hasNext()) {
                minHeap.offer(cursor);
            }
        }

        log.debug("Scatter-gather merged {} events across {} active shards",
                mergedSortedList.size(), shardResults.size());

        return mergedSortedList;
    }

    @Override
    public Map<Integer, Long> getShardDistribution(Long communityId) {
        Map<Integer, Long> distribution = new TreeMap<>();
        for (int i = 0; i < DEFAULT_SHARD_COUNT; i++) {
            String pk = "COMMUNITY_" + communityId + "#shard_" + String.format("%02d", i);
            long count = tableStorage.getOrDefault(pk, Collections.emptyList()).size();
            distribution.put(i, count);
        }
        return distribution;
    }

    @Override
    public void clear() {
        tableStorage.clear();
    }
}

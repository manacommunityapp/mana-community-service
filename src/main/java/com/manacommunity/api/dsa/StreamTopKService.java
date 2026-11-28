package com.manacommunity.api.dsa;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@Slf4j
public class StreamTopKService {

    private static final long WINDOW_MS = 15 * 60 * 1000; // 15 minutes

    private final ConcurrentLinkedDeque<VisitorEvent> eventStream = new ConcurrentLinkedDeque<>();
    private final ConcurrentHashMap<Long, AtomicInteger> societyCounts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, String> societyNames = new ConcurrentHashMap<>();

    public void ingestEvent(long societyId, String societyName, String visitorType) {
        long now = System.currentTimeMillis();
        eventStream.addLast(new VisitorEvent(societyId, visitorType, now));
        societyCounts.computeIfAbsent(societyId, k -> new AtomicInteger(0)).incrementAndGet();
        societyNames.put(societyId, societyName);
        evictExpired(now);
    }

    public void ingestBatch(List<VisitorEvent> events) {
        long now = System.currentTimeMillis();
        for (VisitorEvent event : events) {
            eventStream.addLast(event);
            societyCounts.computeIfAbsent(event.societyId(), k -> new AtomicInteger(0)).incrementAndGet();
            if (event.societyName != null) societyNames.put(event.societyId(), event.societyName);
        }
        evictExpired(now);
    }

    public List<TopSociety> getTopK(int k) {
        evictExpired(System.currentTimeMillis());

        PriorityQueue<Map.Entry<Long, AtomicInteger>> minHeap =
                new PriorityQueue<>(Comparator.comparingInt(e -> e.getValue().get()));

        for (Map.Entry<Long, AtomicInteger> entry : societyCounts.entrySet()) {
            if (entry.getValue().get() <= 0) continue;
            minHeap.offer(entry);
            if (minHeap.size() > k) minHeap.poll();
        }

        List<TopSociety> result = new ArrayList<>();
        while (!minHeap.isEmpty()) {
            Map.Entry<Long, AtomicInteger> entry = minHeap.poll();
            result.add(new TopSociety(entry.getKey(),
                    societyNames.getOrDefault(entry.getKey(), "Society-" + entry.getKey()),
                    entry.getValue().get()));
        }

        result.sort((a, b) -> b.count() - a.count());
        return result;
    }

    public StreamStats getStats() {
        evictExpired(System.currentTimeMillis());
        int totalEvents = eventStream.size();
        int activeSocieties = (int) societyCounts.values().stream().filter(c -> c.get() > 0).count();
        return new StreamStats(totalEvents, activeSocieties, WINDOW_MS / 1000);
    }

    public void clear() {
        eventStream.clear();
        societyCounts.clear();
    }

    private void evictExpired(long now) {
        long cutoff = now - WINDOW_MS;
        while (!eventStream.isEmpty()) {
            VisitorEvent head = eventStream.peekFirst();
            if (head == null || head.timestamp() >= cutoff) break;
            eventStream.pollFirst();
            AtomicInteger count = societyCounts.get(head.societyId());
            if (count != null) count.decrementAndGet();
        }
    }

    public record VisitorEvent(long societyId, String visitorType, long timestamp, String societyName) {
        public VisitorEvent(long societyId, String visitorType, long timestamp) {
            this(societyId, visitorType, timestamp, null);
        }
    }

    public record TopSociety(long societyId, String name, int count) {}

    public record StreamStats(int totalEvents, int activeSocieties, long windowSeconds) {}
}

package com.manacommunity.api.dsa;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dsa")
@RequiredArgsConstructor
public class DsaShowcaseController {

    private final IntervalMergingService intervalService;
    private final TownshipGraphService graphService;
    private final StreamTopKService topKService;
    private final TaggedTtlCache<String> cache;

    // === Interval Merging ===

    @PostMapping("/intervals/available-slots")
    public ResponseEntity<IntervalMergingService.SlotResult> findAvailableSlots(@RequestBody IntervalRequest req) {
        return ResponseEntity.ok(intervalService.computeFullResult(
                req.bookings(), req.blackouts(),
                req.dayStartMinute(), req.dayEndMinute(), req.minDurationMinutes()));
    }

    @PostMapping("/intervals/guard-gaps")
    public ResponseEntity<List<int[]>> findGuardGaps(@RequestBody GuardShiftRequest req) {
        return ResponseEntity.ok(intervalService.findGuardShiftGaps(req.shifts(), req.dayStartMinute(), req.dayEndMinute()));
    }

    // === Graph Routing ===

    @PostMapping("/graph/shortest-path")
    public ResponseEntity<TownshipGraphService.PathResult> findShortestPath(@RequestBody GraphRequest req) {
        Map<String, List<TownshipGraphService.Edge>> graph = graphService.buildDemoGraph();
        return ResponseEntity.ok(graphService.findShortestPath(graph, req.source(), req.destination(), req.isHeavyVehicle()));
    }

    @GetMapping("/graph/nodes")
    public ResponseEntity<Map<String, Object>> getGraphNodes() {
        Map<String, List<TownshipGraphService.Edge>> graph = graphService.buildDemoGraph();
        List<String> nodes = new ArrayList<>(graph.keySet());
        nodes.sort(String::compareTo);
        List<Map<String, Object>> edges = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (var entry : graph.entrySet()) {
            for (var edge : entry.getValue()) {
                String key = entry.getKey().compareTo(edge.to) < 0 ? entry.getKey() + "-" + edge.to : edge.to + "-" + entry.getKey();
                if (seen.add(key)) {
                    edges.add(Map.of("from", entry.getKey(), "to", edge.to, "weight", edge.weight, "heavyAllowed", edge.heavyVehicleAllowed));
                }
            }
        }
        return ResponseEntity.ok(Map.of("nodes", nodes, "edges", edges));
    }

    // === Stream Top-K ===

    @PostMapping("/stream/ingest")
    public ResponseEntity<Map<String, Object>> ingestEvents(@RequestBody StreamIngestRequest req) {
        List<StreamTopKService.VisitorEvent> events = req.events().stream()
                .map(e -> new StreamTopKService.VisitorEvent(e.societyId(), e.visitorType(), System.currentTimeMillis(), e.societyName()))
                .collect(Collectors.toList());
        topKService.ingestBatch(events);
        return ResponseEntity.ok(Map.of("ingested", events.size(), "stats", topKService.getStats()));
    }

    @GetMapping("/stream/top-k")
    public ResponseEntity<Map<String, Object>> getTopK(@RequestParam(defaultValue = "5") int k) {
        return ResponseEntity.ok(Map.of("topK", topKService.getTopK(k), "stats", topKService.getStats()));
    }

    @PostMapping("/stream/clear")
    public ResponseEntity<Map<String, String>> clearStream() {
        topKService.clear();
        return ResponseEntity.ok(Map.of("status", "Stream cleared"));
    }

    // === Tagged TTL Cache ===

    @PostMapping("/cache/put")
    public ResponseEntity<Map<String, String>> cachePut(@RequestBody CachePutRequest req) {
        cache.put(req.key(), req.value(), req.ttlMs(), new HashSet<>(req.tags()));
        return ResponseEntity.ok(Map.of("status", "cached", "key", req.key()));
    }

    @GetMapping("/cache/get")
    public ResponseEntity<Map<String, Object>> cacheGet(@RequestParam String key) {
        String value = cache.get(key);
        return ResponseEntity.ok(Map.of("key", key, "found", value != null, "value", value != null ? value : ""));
    }

    @DeleteMapping("/cache/invalidate-tag")
    public ResponseEntity<Map<String, Object>> invalidateTag(@RequestParam String tag) {
        int count = cache.invalidateByTag(tag);
        return ResponseEntity.ok(Map.of("tag", tag, "keysInvalidated", count));
    }

    @GetMapping("/cache/stats")
    public ResponseEntity<TaggedTtlCache.CacheStats> cacheStats() {
        return ResponseEntity.ok(cache.getStats());
    }

    @PostMapping("/cache/clear")
    public ResponseEntity<Map<String, String>> cacheClear() {
        cache.clear();
        return ResponseEntity.ok(Map.of("status", "Cache cleared"));
    }

    // Request DTOs
    public record IntervalRequest(List<int[]> bookings, List<int[]> blackouts, int dayStartMinute, int dayEndMinute, int minDurationMinutes) {}
    public record GuardShiftRequest(List<int[]> shifts, int dayStartMinute, int dayEndMinute) {}
    public record GraphRequest(String source, String destination, boolean isHeavyVehicle) {}
    public record StreamEvent(long societyId, String societyName, String visitorType) {}
    public record StreamIngestRequest(List<StreamEvent> events) {}
    public record CachePutRequest(String key, String value, long ttlMs, List<String> tags) {}
}

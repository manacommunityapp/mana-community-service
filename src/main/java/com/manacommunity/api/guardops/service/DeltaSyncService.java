package com.manacommunity.api.guardops.service;

import com.manacommunity.api.guardops.entity.DeltaSyncCursor;
import com.manacommunity.api.guardops.entity.DeltaSyncCursor.*;
import com.manacommunity.api.guardops.repository.DeltaSyncCursorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DeltaSyncService {

    private final DeltaSyncCursorRepository cursorRepo;

    public record SyncDashboard(
            long totalDevices, long upToDateDevices, long staleDevices, long errorDevices,
            long totalRecordsSynced, long totalTombstonesPurged, long totalBytesTransferred,
            double avgBloomFilterFpr, long totalCacheUsedMb, long totalEvictedRecords,
            List<DeltaSyncCursor> cursors,
            SyncProtocolSpec protocolSpec
    ) {}

    public record SyncProtocolSpec(
            String endpoint, String cursorParam, String tombstoneStrategy,
            String bloomFilterSpec, String cacheEvictionPolicy,
            long maxDailyDataBudgetMb, long estimatedDailyUsageMb,
            int residentCount, int vehicleCount, int dailyHelpCount,
            String compressionAlgorithm, String conflictResolution
    ) {}

    public SyncDashboard getDashboard() {
        List<DeltaSyncCursor> cursors = cursorRepo.findAll();

        long upToDate = cursors.stream().filter(c -> c.getStatus() == SyncStatus.UP_TO_DATE).count();
        long stale = cursors.stream().filter(c -> c.getStatus() == SyncStatus.STALE).count();
        long error = cursors.stream().filter(c -> c.getStatus() == SyncStatus.ERROR).count();

        long totalSynced = cursors.stream().mapToLong(DeltaSyncCursor::getRecordsSynced).sum();
        long totalTombstones = cursors.stream().mapToLong(DeltaSyncCursor::getTombstonesPurged).sum();
        long totalBytes = cursors.stream().mapToLong(DeltaSyncCursor::getBytesTransferred).sum();
        double avgFpr = cursors.stream().mapToDouble(DeltaSyncCursor::getBloomFilterFalsePositiveRate).average().orElse(0);
        long totalCache = cursors.stream().mapToLong(DeltaSyncCursor::getUsedCacheMb).sum();
        long totalEvicted = cursors.stream().mapToLong(DeltaSyncCursor::getEvictedRecords).sum();

        long devices = cursors.stream().map(DeltaSyncCursor::getDeviceId).distinct().count();

        SyncProtocolSpec spec = new SyncProtocolSpec(
                "GET /api/guard-ops/sync?entity={type}&since_cursor={cursor}&device_id={id}",
                "since_cursor (ISO-8601 timestamp + sequence number)",
                "Soft-delete with tombstone markers. Tombstones retained 30 days, then purged. Client receives tombstone records with _deleted=true flag.",
                "Bloom filter per entity: 10-bit-per-element, 3 hash functions (murmur3). FPR ~0.82%. Client sends Bloom filter of known IDs; server returns only IDs NOT in the filter (new/updated records).",
                "LRU with priority tiers: Tier 1 (residents, expected visitors) = never evict; Tier 2 (vehicles, daily help) = evict after 7 days inactive; Tier 3 (historical visitors) = evict after 24 hours",
                5, 3,
                12000, 8000, 3000,
                "gzip (Content-Encoding: gzip) — 60-70% compression on JSON payloads",
                "Last-write-wins with server timestamp. Conflicts logged for supervisor review."
        );

        return new SyncDashboard(devices, upToDate, stale, error, totalSynced, totalTombstones, totalBytes,
                avgFpr, totalCache, totalEvicted, cursors, spec);
    }
}

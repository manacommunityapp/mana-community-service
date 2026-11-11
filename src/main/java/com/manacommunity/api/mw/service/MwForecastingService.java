package com.manacommunity.api.mw.service;

import com.manacommunity.api.mw.dto.CreateSnapshotRequest;
import com.manacommunity.api.mw.dto.UpdateForecastConfigRequest;
import com.manacommunity.api.mw.entity.MwForecastConfig;
import com.manacommunity.api.mw.entity.MwForecastProjection;
import com.manacommunity.api.mw.entity.MwForecastSnapshot;
import com.manacommunity.api.mw.repository.MwForecastConfigRepository;
import com.manacommunity.api.mw.repository.MwForecastProjectionRepository;
import com.manacommunity.api.mw.repository.MwForecastSnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class MwForecastingService {

    private final MwForecastConfigRepository configRepo;
    private final MwForecastSnapshotRepository snapshotRepo;
    private final MwForecastProjectionRepository projectionRepo;

    public MwForecastConfig getOrCreateConfig(Long orgId) {
        return configRepo.findByOrganizationId(orgId)
                .orElseGet(() -> configRepo.save(MwForecastConfig.builder().organizationId(orgId).build()));
    }

    public MwForecastConfig updateConfig(Long orgId, UpdateForecastConfigRequest req) {
        MwForecastConfig config = getOrCreateConfig(orgId);
        if (req.getHorizonWeeks() != null) config.setHorizonWeeks(req.getHorizonWeeks());
        if (req.getMinWorkersPerShift() != null) config.setMinWorkersPerShift(req.getMinWorkersPerShift());
        if (req.getPeakDayMultiplier() != null) config.setPeakDayMultiplier(req.getPeakDayMultiplier());
        if (req.getSourceModules() != null) config.setSourceModules(req.getSourceModules());
        if (req.getAutoSnapshotEnabled() != null) config.setAutoSnapshotEnabled(req.getAutoSnapshotEnabled());
        return configRepo.save(config);
    }

    public MwForecastSnapshot upsertSnapshot(Long orgId, CreateSnapshotRequest req) {
        MwForecastSnapshot snapshot = snapshotRepo
                .findByOrganizationIdAndWeekStartDateAndSourceModule(orgId, req.getWeekStartDate(), req.getSourceModule())
                .orElse(MwForecastSnapshot.builder()
                        .organizationId(orgId)
                        .weekStartDate(req.getWeekStartDate())
                        .sourceModule(req.getSourceModule())
                        .build());
        snapshot.setDemandUnits(req.getDemandUnits());
        snapshot.setFulfilledUnits(req.getFulfilledUnits());
        snapshot.setAvgPayRate(req.getAvgPayRate());
        snapshot.setWorkerCount(req.getWorkerCount());
        snapshot.setNotes(req.getNotes());
        return snapshotRepo.save(snapshot);
    }

    public List<MwForecastSnapshot> getSnapshots(Long orgId) {
        return snapshotRepo.findByOrganizationIdOrderByWeekStartDateDesc(orgId);
    }

    /**
     * Trend-based projection: 4-week rolling average per module,
     * peak multiplier applied beyond week 2, confidence -10% per week out.
     */
    public List<MwForecastProjection> generateProjections(Long orgId) {
        MwForecastConfig config = getOrCreateConfig(orgId);
        LocalDate since = LocalDate.now().minusWeeks(4);
        List<MwForecastSnapshot> recentSnapshots = snapshotRepo.findRecent(orgId, since);

        Map<String, List<MwForecastSnapshot>> byModule = recentSnapshots.stream()
                .collect(Collectors.groupingBy(MwForecastSnapshot::getSourceModule));

        projectionRepo.deleteByOrganizationIdAndProjectedWeekAfter(orgId, LocalDate.now().minusDays(1));

        List<MwForecastProjection> projections = new ArrayList<>();
        int horizon = config.getHorizonWeeks() != null ? config.getHorizonWeeks() : 4;

        for (Map.Entry<String, List<MwForecastSnapshot>> entry : byModule.entrySet()) {
            String module = entry.getKey();
            List<MwForecastSnapshot> snapshots = entry.getValue();
            if (snapshots.isEmpty()) continue;

            double avgDemand = snapshots.stream()
                    .mapToInt(MwForecastSnapshot::getDemandUnits)
                    .average().orElse(0);

            for (int w = 1; w <= horizon; w++) {
                LocalDate projectedWeek = LocalDate.now().plusWeeks(w);
                double multiplier = (w > 2 && config.getPeakDayMultiplier() != null)
                        ? config.getPeakDayMultiplier().doubleValue() : 1.0;
                double projected = avgDemand * multiplier;
                int confidence = Math.max(30, 80 - (w - 1) * 10);

                MwForecastProjection proj = projectionRepo.save(MwForecastProjection.builder()
                        .organizationId(orgId)
                        .projectedWeek(projectedWeek)
                        .sourceModule(module)
                        .projectedDemand((int) Math.round(projected))
                        .confidencePct(confidence)
                        .build());
                projections.add(proj);
            }
        }
        return projections;
    }

    public List<MwForecastProjection> getProjections(Long orgId) {
        return projectionRepo.findByOrganizationIdOrderByProjectedWeekAsc(orgId);
    }

    public Map<String, Object> getForecastSummary(Long orgId) {
        MwForecastConfig config = getOrCreateConfig(orgId);
        List<MwForecastSnapshot> snapshots = snapshotRepo.findByOrganizationIdOrderByWeekStartDateDesc(orgId);
        List<MwForecastProjection> projections = projectionRepo.findByOrganizationIdOrderByProjectedWeekAsc(orgId);

        Map<String, Integer> gapByModule = new LinkedHashMap<>();
        for (MwForecastSnapshot s : snapshots) {
            gapByModule.merge(s.getSourceModule(),
                    s.getDemandUnits() - s.getFulfilledUnits(), Integer::sum);
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("config", config);
        summary.put("recentSnapshots", snapshots.stream().limit(12).collect(Collectors.toList()));
        summary.put("projections", projections);
        summary.put("gapByModule", gapByModule);
        return summary;
    }
}

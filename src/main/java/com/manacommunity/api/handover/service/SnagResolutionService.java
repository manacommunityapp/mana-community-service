package com.manacommunity.api.handover.service;

import com.manacommunity.api.handover.entity.FlatInspection;
import com.manacommunity.api.handover.entity.FlatInspection.*;
import com.manacommunity.api.handover.entity.SnagItem;
import com.manacommunity.api.handover.entity.SnagItem.*;
import com.manacommunity.api.handover.repository.FlatInspectionRepository;
import com.manacommunity.api.handover.repository.SnagItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SnagResolutionService {

    private final SnagItemRepository snagRepo;
    private final FlatInspectionRepository inspectionRepo;

    @Transactional
    public SnagItem logSnag(String flatNumber, String towerName, int floor,
                            SnagCategory category, SnagSeverity severity, RoomLocation room,
                            String description, String photoUrl, String reportedBy, String phone) {
        String code = "SNAG-" + towerName.replace(" ", "") + "-" + flatNumber + "-" + System.currentTimeMillis() % 100000;
        boolean blocker = severity == SnagSeverity.CRITICAL || severity == SnagSeverity.MAJOR;

        SnagItem snag = SnagItem.builder()
                .communityId(1L).projectId(1L)
                .flatNumber(flatNumber).towerName(towerName).floorNumber(floor)
                .snagCode(code).category(category).severity(severity)
                .status(SnagStatus.LOGGED).roomLocation(room)
                .description(description).photoBeforeUrl(photoUrl)
                .reportedByName(reportedBy).reportedByPhone(phone)
                .reportedAt(LocalDateTime.now())
                .possessionBlocker(blocker)
                .reworkCount(0).reInspectionCount(0)
                .build();

        return snagRepo.save(snag);
    }

    @Transactional
    public SnagItem assignToSubcontractor(Long snagId, String subId, String subName, String trade, int targetDays) {
        SnagItem snag = snagRepo.findById(snagId).orElseThrow(() -> new RuntimeException("Snag not found"));
        snag.setAssignedSubcontractorId(subId);
        snag.setAssignedSubcontractorName(subName);
        snag.setSubcontractorTrade(trade);
        snag.setAssignedAt(LocalDateTime.now());
        snag.setTargetResolutionDate(LocalDate.now().plusDays(targetDays));
        snag.setStatus(SnagStatus.ASSIGNED);
        return snagRepo.save(snag);
    }

    @Transactional
    public SnagItem startWork(Long snagId) {
        SnagItem snag = snagRepo.findById(snagId).orElseThrow(() -> new RuntimeException("Snag not found"));
        snag.setStatus(SnagStatus.WORK_IN_PROGRESS);
        return snagRepo.save(snag);
    }

    @Transactional
    public SnagItem submitForReInspection(Long snagId, String photoAfterUrl, String notes) {
        SnagItem snag = snagRepo.findById(snagId).orElseThrow(() -> new RuntimeException("Snag not found"));
        snag.setStatus(SnagStatus.RE_INSPECTION_PENDING);
        snag.setPhotoAfterUrl(photoAfterUrl);
        snag.setResolutionNotes(notes);
        snag.setResolvedAt(LocalDateTime.now());
        return snagRepo.save(snag);
    }

    @Transactional
    public SnagItem reInspect(Long snagId, boolean passed, String inspectorName) {
        SnagItem snag = snagRepo.findById(snagId).orElseThrow(() -> new RuntimeException("Snag not found"));
        snag.setReInspectionCount(snag.getReInspectionCount() + 1);
        snag.setReInspectedByName(inspectorName);
        snag.setReInspectionDate(LocalDateTime.now());

        if (passed) {
            snag.setStatus(SnagStatus.RE_INSPECTION_PASSED);
            snag.setPossessionBlocker(false);
        } else {
            snag.setStatus(SnagStatus.REWORK_NEEDED);
            snag.setReworkCount(snag.getReworkCount() + 1);
        }
        return snagRepo.save(snag);
    }

    @Transactional
    public SnagItem closeSnag(Long snagId) {
        SnagItem snag = snagRepo.findById(snagId).orElseThrow(() -> new RuntimeException("Snag not found"));
        snag.setStatus(SnagStatus.CLOSED);
        return snagRepo.save(snag);
    }

    public boolean canSignOffPossession(String flatNumber, String towerName) {
        List<SnagItem> snags = snagRepo.findByFlatNumberAndTowerName(flatNumber, towerName);
        return snags.stream().noneMatch(s -> s.isPossessionBlocker()
                && s.getStatus() != SnagStatus.CLOSED
                && s.getStatus() != SnagStatus.RE_INSPECTION_PASSED);
    }

    @Transactional
    public FlatInspection conductInspection(String flatNumber, String towerName, int floor,
                                              InspectionPhase phase, String inspectorName, String buyerName) {
        List<SnagItem> snags = snagRepo.findByFlatNumberAndTowerName(flatNumber, towerName);
        long criticalOpen = snags.stream().filter(s -> s.getSeverity() == SnagSeverity.CRITICAL && isOpen(s)).count();
        long majorOpen = snags.stream().filter(s -> s.getSeverity() == SnagSeverity.MAJOR && isOpen(s)).count();
        long minorOpen = snags.stream().filter(s -> (s.getSeverity() == SnagSeverity.MINOR || s.getSeverity() == SnagSeverity.COSMETIC) && isOpen(s)).count();

        boolean blocked = criticalOpen > 0;
        InspectionOutcome outcome;
        if (criticalOpen > 0) outcome = InspectionOutcome.FAIL;
        else if (majorOpen > 0) outcome = InspectionOutcome.CONDITIONAL_PASS;
        else outcome = InspectionOutcome.PASS;

        FlatInspection insp = FlatInspection.builder()
                .communityId(1L).projectId(1L)
                .flatNumber(flatNumber).towerName(towerName).floorNumber(floor)
                .phase(phase).outcome(outcome)
                .inspectorName(inspectorName).inspectorRole("QA Engineer")
                .buyerName(buyerName)
                .scheduledAt(LocalDateTime.now()).startedAt(LocalDateTime.now())
                .completedAt(LocalDateTime.now().plusMinutes(90)).durationMinutes(90)
                .totalSnagsFound(snags.size())
                .criticalSnagsOpen((int) criticalOpen).majorSnagsOpen((int) majorOpen).minorSnagsOpen((int) minorOpen)
                .possessionSignOffBlocked(blocked)
                .build();

        return inspectionRepo.save(insp);
    }

    private boolean isOpen(SnagItem s) {
        return s.getStatus() != SnagStatus.CLOSED && s.getStatus() != SnagStatus.RE_INSPECTION_PASSED;
    }

    public SnagDashboard getDashboard() {
        List<SnagItem> all = snagRepo.findAll();
        List<FlatInspection> inspections = inspectionRepo.findAll();

        Map<String, Long> byCategory = all.stream().collect(Collectors.groupingBy(s -> s.getCategory().name(), Collectors.counting()));
        Map<String, Long> byStatus = all.stream().collect(Collectors.groupingBy(s -> s.getStatus().name(), Collectors.counting()));
        Map<String, Long> bySeverity = all.stream().collect(Collectors.groupingBy(s -> s.getSeverity().name(), Collectors.counting()));
        Map<String, Long> byTower = all.stream().collect(Collectors.groupingBy(SnagItem::getTowerName, Collectors.counting()));

        long openBlockers = all.stream().filter(s -> s.isPossessionBlocker() && isOpen(s)).count();
        long totalReworks = all.stream().mapToInt(SnagItem::getReworkCount).sum();
        double avgResolveTime = all.stream().filter(s -> s.getResolvedAt() != null && s.getReportedAt() != null)
                .mapToLong(s -> java.time.Duration.between(s.getReportedAt(), s.getResolvedAt()).toHours())
                .average().orElse(0);

        // Subcontractor leaderboard
        Map<String, SubcontractorScore> subScores = all.stream()
                .filter(s -> s.getAssignedSubcontractorName() != null)
                .collect(Collectors.groupingBy(SnagItem::getAssignedSubcontractorName))
                .entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> {
                    List<SnagItem> snags = e.getValue();
                    long closed = snags.stream().filter(s -> !isOpen(s)).count();
                    double reworkRate = snags.stream().mapToInt(SnagItem::getReworkCount).sum() * 100.0 / Math.max(snags.size(), 1);
                    return new SubcontractorScore(e.getKey(), snags.get(0).getSubcontractorTrade(),
                            snags.size(), closed, reworkRate);
                }));

        long completedHandovers = inspections.stream().filter(FlatInspection::isBuyerSignedOff).count();

        return new SnagDashboard(all.size(), openBlockers, totalReworks, avgResolveTime,
                byCategory, byStatus, bySeverity, byTower, subScores,
                inspections.size(), completedHandovers,
                all, snagRepo.findPossessionBlockers(), inspections);
    }

    public record SubcontractorScore(String name, String trade, long totalSnags, long closedSnags, double reworkRatePct) {}
    public record SnagDashboard(int totalSnags, long openBlockers, long totalReworks, double avgResolveHours,
                                  Map<String, Long> byCategory, Map<String, Long> byStatus,
                                  Map<String, Long> bySeverity, Map<String, Long> byTower,
                                  Map<String, SubcontractorScore> subcontractorScores,
                                  int totalInspections, long completedHandovers,
                                  List<SnagItem> allSnags, List<SnagItem> possessionBlockers,
                                  List<FlatInspection> inspections) {}
}

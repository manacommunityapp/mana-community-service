package com.manacommunity.api.assetaudit.service;

import com.manacommunity.api.assetaudit.entity.HousekeepingTask;
import com.manacommunity.api.assetaudit.entity.HousekeepingTask.*;
import com.manacommunity.api.assetaudit.repository.HousekeepingTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class HousekeepingWorkflowService {

    private final HousekeepingTaskRepository taskRepo;

    private static final int ESCALATION_THRESHOLD_MINUTES = 15;
    private static final int CRITICAL_ESCALATION_MINUTES = 30;

    @Transactional
    public HousekeepingTask reportDirtyLocation(String qrCode, String residentRemarks) {
        HousekeepingTask existing = taskRepo.findByQrCode(qrCode).orElse(null);

        if (existing != null && existing.getStatus() != TaskStatus.COMPLETED && existing.getStatus() != TaskStatus.VERIFIED) {
            existing.setResidentReported(true);
            existing.setResidentReportQrScan(qrCode);
            existing.setResidentReportedAt(LocalDateTime.now());
            existing.setResidentRemarks(residentRemarks);
            existing.setPriority(TaskPriority.URGENT);
            existing.setStatus(TaskStatus.REPORTED_DIRTY);
            return taskRepo.save(existing);
        }

        HousekeepingTask task = HousekeepingTask.builder()
                .communityId(1L)
                .qrCode(qrCode)
                .locationId("LOC-" + qrCode)
                .locationName(inferLocationName(qrCode))
                .locationType(inferLocationType(qrCode))
                .towerName(inferTower(qrCode))
                .status(TaskStatus.REPORTED_DIRTY)
                .priority(TaskPriority.URGENT)
                .residentReported(true)
                .residentReportQrScan(qrCode)
                .residentReportedAt(LocalDateTime.now())
                .residentRemarks(residentRemarks)
                .escalated(false)
                .escalationLevel(0)
                .build();

        return taskRepo.save(task);
    }

    @Transactional
    public HousekeepingTask assignTask(Long taskId, String staffId, String staffName) {
        HousekeepingTask task = taskRepo.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        task.setAssignedStaffId(staffId);
        task.setAssignedStaffName(staffName);
        task.setStatus(TaskStatus.ASSIGNED);
        return taskRepo.save(task);
    }

    @Transactional
    public HousekeepingTask startTask(Long taskId) {
        HousekeepingTask task = taskRepo.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setStartedAt(LocalDateTime.now());
        return taskRepo.save(task);
    }

    @Transactional
    public HousekeepingTask completeTask(Long taskId, String completedItems, String photoAfterUrl) {
        HousekeepingTask task = taskRepo.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        task.setStatus(TaskStatus.COMPLETED);
        task.setCompletedAt(LocalDateTime.now());
        task.setCompletedItemsJson(completedItems);
        task.setPhotoAfterUrl(photoAfterUrl);
        if (task.getStartedAt() != null) {
            task.setDurationMinutes(ChronoUnit.MINUTES.between(task.getStartedAt(), task.getCompletedAt()));
        }
        return taskRepo.save(task);
    }

    @Transactional
    public List<HousekeepingTask> checkAndEscalate() {
        List<HousekeepingTask> urgent = taskRepo.findPendingUrgentTasks();
        List<HousekeepingTask> escalated = new ArrayList<>();

        for (HousekeepingTask task : urgent) {
            if (task.getResidentReportedAt() == null) continue;
            long minutesSinceReport = ChronoUnit.MINUTES.between(task.getResidentReportedAt(), LocalDateTime.now());

            if (minutesSinceReport >= CRITICAL_ESCALATION_MINUTES && task.getEscalationLevel() < 2) {
                task.setEscalationLevel(2);
                task.setEscalated(true);
                task.setEscalationReason("Critical: unresolved for " + minutesSinceReport + " min — escalated to Facility Manager");
                task.setEscalatedAt(LocalDateTime.now());
                task.setPriority(TaskPriority.CRITICAL);
                task.setStatus(TaskStatus.ESCALATED);
                escalated.add(taskRepo.save(task));
            } else if (minutesSinceReport >= ESCALATION_THRESHOLD_MINUTES && task.getEscalationLevel() < 1) {
                task.setEscalationLevel(1);
                task.setEscalated(true);
                task.setEscalationReason("Unresolved for " + minutesSinceReport + " min — escalated to Supervisor");
                task.setEscalatedAt(LocalDateTime.now());
                task.setPriority(TaskPriority.HIGH);
                task.setStatus(TaskStatus.ESCALATED);
                escalated.add(taskRepo.save(task));
            }
        }
        return escalated;
    }

    public HousekeepingDashboard getDashboard(Long communityId) {
        List<HousekeepingTask> all = taskRepo.findAll();
        List<HousekeepingTask> active = taskRepo.findActiveTasks(communityId);
        List<HousekeepingTask> escalations = taskRepo.findActiveEscalations();

        Map<String, Long> byStatus = all.stream()
                .collect(Collectors.groupingBy(t -> t.getStatus().name(), Collectors.counting()));
        Map<String, Long> byPriority = active.stream()
                .collect(Collectors.groupingBy(t -> t.getPriority().name(), Collectors.counting()));
        Map<String, Long> byLocation = all.stream()
                .collect(Collectors.groupingBy(t -> t.getLocationType().name(), Collectors.counting()));

        double avgDuration = all.stream()
                .filter(t -> t.getDurationMinutes() > 0)
                .mapToLong(HousekeepingTask::getDurationMinutes)
                .average().orElse(0);

        long residentReports = all.stream().filter(HousekeepingTask::isResidentReported).count();
        long completedToday = all.stream()
                .filter(t -> t.getCompletedAt() != null && t.getCompletedAt().toLocalDate().equals(LocalDateTime.now().toLocalDate()))
                .count();

        // Staff performance
        Map<String, StaffPerformance> staffPerf = all.stream()
                .filter(t -> t.getAssignedStaffName() != null)
                .collect(Collectors.groupingBy(HousekeepingTask::getAssignedStaffName))
                .entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> {
                    List<HousekeepingTask> tasks = e.getValue();
                    long completed = tasks.stream().filter(t -> t.getStatus() == TaskStatus.COMPLETED || t.getStatus() == TaskStatus.VERIFIED).count();
                    double avg = tasks.stream().filter(t -> t.getDurationMinutes() > 0).mapToLong(HousekeepingTask::getDurationMinutes).average().orElse(0);
                    double rating = tasks.stream().filter(t -> t.getRatingScore() > 0).mapToDouble(HousekeepingTask::getRatingScore).average().orElse(0);
                    return new StaffPerformance(e.getKey(), tasks.size(), completed, avg, rating);
                }));

        // Tower schedule
        Map<String, List<HousekeepingTask>> byTower = active.stream()
                .filter(t -> t.getTowerName() != null)
                .collect(Collectors.groupingBy(HousekeepingTask::getTowerName));

        return new HousekeepingDashboard(all.size(), active.size(), escalations.size(),
                byStatus, byPriority, byLocation, avgDuration, residentReports,
                completedToday, staffPerf, byTower, active, escalations);
    }

    public String generateChecklist(LocationType type) {
        return switch (type) {
            case TOWER_LOBBY -> "[\"Sweep and mop floor\",\"Wipe elevator buttons and door handles\",\"Clean glass panels\",\"Empty trash bins\",\"Check light fixtures\",\"Dust fire extinguisher area\",\"Clean intercom panel\",\"Polish metal railings\"]";
            case CLUBHOUSE_RESTROOM -> "[\"Scrub toilets and urinals\",\"Clean wash basins and mirrors\",\"Mop floor with disinfectant\",\"Refill soap and tissue dispensers\",\"Empty sanitary bins\",\"Check flush mechanisms\",\"Spray air freshener\",\"Wipe door handles\",\"Check ventilation fan\"]";
            case GYM -> "[\"Wipe all equipment surfaces\",\"Mop rubber flooring\",\"Clean mirrors\",\"Check water cooler\",\"Sanitize yoga mats\",\"Empty towel bin\",\"Restock paper towels\"]";
            case POOL_AREA -> "[\"Skim pool surface\",\"Check chlorine levels\",\"Clean deck area\",\"Wipe loungers\",\"Check shower heads\",\"Clean changing rooms\"]";
            default -> "[\"General cleaning\",\"Trash removal\",\"Surface wipe-down\",\"Floor mopping\"]";
        };
    }

    private String inferLocationName(String qrCode) {
        if (qrCode.startsWith("LOBBY")) return "Tower Lobby " + qrCode.substring(5);
        if (qrCode.startsWith("REST")) return "Clubhouse Restroom " + qrCode.substring(4);
        return "Location " + qrCode;
    }

    private LocationType inferLocationType(String qrCode) {
        if (qrCode.startsWith("LOBBY")) return LocationType.TOWER_LOBBY;
        if (qrCode.startsWith("REST")) return LocationType.CLUBHOUSE_RESTROOM;
        if (qrCode.startsWith("GYM")) return LocationType.GYM;
        return LocationType.TOWER_LOBBY;
    }

    private String inferTower(String qrCode) {
        if (qrCode.contains("-A")) return "Tower A";
        if (qrCode.contains("-B")) return "Tower B";
        if (qrCode.contains("-C")) return "Tower C";
        return "Tower A";
    }

    public record StaffPerformance(String staffName, long totalTasks, long completedTasks,
                                     double avgDurationMin, double avgRating) {}
    public record HousekeepingDashboard(int totalTasks, int activeTasks, int activeEscalations,
                                          Map<String, Long> byStatus, Map<String, Long> byPriority,
                                          Map<String, Long> byLocationType, double avgDurationMin,
                                          long residentReports, long completedToday,
                                          Map<String, StaffPerformance> staffPerformance,
                                          Map<String, List<HousekeepingTask>> byTower,
                                          List<HousekeepingTask> activeTasks2, List<HousekeepingTask> escalations) {}
}

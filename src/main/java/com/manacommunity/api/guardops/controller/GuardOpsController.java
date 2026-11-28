package com.manacommunity.api.guardops.controller;

import com.manacommunity.api.guardops.entity.*;
import com.manacommunity.api.guardops.entity.DeltaSyncCursor.*;
import com.manacommunity.api.guardops.entity.GamificationEvent.*;
import com.manacommunity.api.guardops.entity.GuardProfile.*;
import com.manacommunity.api.guardops.entity.ShiftRoster.*;
import com.manacommunity.api.guardops.repository.*;
import com.manacommunity.api.guardops.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@RestController
@RequestMapping("/api/guard-ops")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class GuardOpsController {

    private final ShiftHandoverService handoverService;
    private final DeltaSyncService syncService;
    private final GuardProfileRepository guardRepo;
    private final ShiftRosterRepository rosterRepo;
    private final ShiftHandoverRepository handoverRepo;
    private final HandoverChecklistItemRepository checklistRepo;
    private final DeltaSyncCursorRepository cursorRepo;
    private final GamificationEventRepository gamificationRepo;

    @GetMapping("/handover/dashboard")
    public ResponseEntity<?> getHandoverDashboard() {
        return ResponseEntity.ok(handoverService.getDashboard());
    }

    @GetMapping("/sync/dashboard")
    public ResponseEntity<?> getSyncDashboard() {
        return ResponseEntity.ok(syncService.getDashboard());
    }

    @PostMapping("/handover/initiate")
    public ResponseEntity<?> initiateHandover(@RequestParam Long outgoingGuardId, @RequestParam Long incomingGuardId,
                                                @RequestParam String gateId, @RequestParam String gateName,
                                                @RequestParam int visitors, @RequestParam int packages,
                                                @RequestParam int incidents, @RequestParam int deliveries,
                                                @RequestParam int brokenEquipment) {
        return ResponseEntity.ok(handoverService.initiateHandover(outgoingGuardId, incomingGuardId, gateId, gateName,
                visitors, packages, incidents, deliveries, brokenEquipment));
    }

    @PostMapping("/handover/{id}/acknowledge")
    public ResponseEntity<?> acknowledge(@PathVariable Long id, @RequestParam Long guardId, @RequestParam boolean isIncoming) {
        return ResponseEntity.ok(handoverService.acknowledgeHandover(id, guardId, isIncoming));
    }

    @GetMapping("/guards")
    public ResponseEntity<?> getAllGuards() { return ResponseEntity.ok(guardRepo.findAll()); }

    @GetMapping("/leaderboard")
    public ResponseEntity<?> getLeaderboard() { return ResponseEntity.ok(gamificationRepo.findAll()); }

    @PostMapping("/seed-demo")
    public ResponseEntity<?> seedDemoData() {
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        // --- Guard Profiles ---
        GuardProfile rajan = guardRepo.save(GuardProfile.builder()
                .communityId("COMM-001").guardName("Rajan Kumar").guardCode("GRD-001").phone("9876500101")
                .agencyName("SecureForce Agency").rank(GuardRank.SENIOR_GUARD)
                .assignedGateId("GATE-MAIN").assignedGateName("Main Gate")
                .joiningDate(LocalDate.of(2023, 3, 15)).biometricRegistered(true).isActive(true)
                .totalShiftsCompleted(892).incidentsClosed(47).gamificationPoints(4250)
                .performanceTier(GuardTier.PLATINUM).build());

        GuardProfile sunil = guardRepo.save(GuardProfile.builder()
                .communityId("COMM-001").guardName("Sunil Verma").guardCode("GRD-002").phone("9876500102")
                .agencyName("SecureForce Agency").rank(GuardRank.GUARD)
                .assignedGateId("GATE-MAIN").assignedGateName("Main Gate")
                .joiningDate(LocalDate.of(2024, 1, 10)).biometricRegistered(true).isActive(true)
                .totalShiftsCompleted(340).incidentsClosed(12).gamificationPoints(1850)
                .performanceTier(GuardTier.GOLD).build());

        GuardProfile deepak = guardRepo.save(GuardProfile.builder()
                .communityId("COMM-001").guardName("Deepak Singh").guardCode("GRD-003").phone("9876500103")
                .agencyName("SafeGuard Services").rank(GuardRank.GUARD)
                .assignedGateId("GATE-SIDE").assignedGateName("Side Gate")
                .joiningDate(LocalDate.of(2024, 6, 1)).biometricRegistered(true).isActive(true)
                .totalShiftsCompleted(180).incidentsClosed(5).gamificationPoints(920)
                .performanceTier(GuardTier.SILVER).build());

        GuardProfile manoj = guardRepo.save(GuardProfile.builder()
                .communityId("COMM-001").guardName("Manoj Tiwari").guardCode("GRD-004").phone("9876500104")
                .agencyName("SafeGuard Services").rank(GuardRank.SHIFT_SUPERVISOR)
                .assignedGateId("GATE-MAIN").assignedGateName("Main Gate")
                .joiningDate(LocalDate.of(2022, 8, 20)).biometricRegistered(true).isActive(true)
                .totalShiftsCompleted(1250).incidentsClosed(89).gamificationPoints(6100)
                .performanceTier(GuardTier.PLATINUM).build());

        GuardProfile anil = guardRepo.save(GuardProfile.builder()
                .communityId("COMM-001").guardName("Anil Yadav").guardCode("GRD-005").phone("9876500105")
                .agencyName("SecureForce Agency").rank(GuardRank.GUARD)
                .assignedGateId("GATE-SIDE").assignedGateName("Side Gate")
                .joiningDate(LocalDate.of(2025, 2, 1)).biometricRegistered(false).isActive(true)
                .totalShiftsCompleted(85).incidentsClosed(2).gamificationPoints(380)
                .performanceTier(GuardTier.BRONZE).build());

        // --- Shift Rosters (today) ---
        rosterRepo.saveAll(List.of(
                ShiftRoster.builder().communityId("COMM-001").guardId(rajan.getId()).guardName("Rajan Kumar")
                        .gateId("GATE-MAIN").gateName("Main Gate").shiftType(ShiftType.DAY_SHIFT)
                        .shiftDate(today).scheduledStart(LocalTime.of(6, 0)).scheduledEnd(LocalTime.of(14, 0))
                        .actualClockIn(LocalTime.of(5, 55)).clockInMethod(ClockInMethod.BIOMETRIC)
                        .biometricVerified(true).lateArrival(false).lateMinutes(0)
                        .status(ShiftStatus.HANDOVER_PENDING).build(),
                ShiftRoster.builder().communityId("COMM-001").guardId(sunil.getId()).guardName("Sunil Verma")
                        .gateId("GATE-MAIN").gateName("Main Gate").shiftType(ShiftType.DAY_SHIFT)
                        .shiftDate(today).scheduledStart(LocalTime.of(14, 0)).scheduledEnd(LocalTime.of(22, 0))
                        .actualClockIn(LocalTime.of(14, 8)).clockInMethod(ClockInMethod.SELFIE_GPS)
                        .biometricVerified(false).lateArrival(true).lateMinutes(8)
                        .status(ShiftStatus.ON_DUTY).build(),
                ShiftRoster.builder().communityId("COMM-001").guardId(deepak.getId()).guardName("Deepak Singh")
                        .gateId("GATE-SIDE").gateName("Side Gate").shiftType(ShiftType.DAY_SHIFT)
                        .shiftDate(today).scheduledStart(LocalTime.of(6, 0)).scheduledEnd(LocalTime.of(18, 0))
                        .actualClockIn(LocalTime.of(6, 2)).clockInMethod(ClockInMethod.BIOMETRIC)
                        .biometricVerified(true).lateArrival(false).lateMinutes(0)
                        .status(ShiftStatus.ON_DUTY).build(),
                ShiftRoster.builder().communityId("COMM-001").guardId(manoj.getId()).guardName("Manoj Tiwari")
                        .gateId("GATE-MAIN").gateName("Main Gate").shiftType(ShiftType.NIGHT_SHIFT)
                        .shiftDate(today).scheduledStart(LocalTime.of(22, 0)).scheduledEnd(LocalTime.of(6, 0))
                        .status(ShiftStatus.SCHEDULED).build(),
                ShiftRoster.builder().communityId("COMM-001").guardId(anil.getId()).guardName("Anil Yadav")
                        .gateId("GATE-SIDE").gateName("Side Gate").shiftType(ShiftType.NIGHT_SHIFT)
                        .shiftDate(today).scheduledStart(LocalTime.of(18, 0)).scheduledEnd(LocalTime.of(6, 0))
                        .status(ShiftStatus.SCHEDULED).build()
        ));

        // --- Shift Handover (8 PM scenario) ---
        ShiftHandover h1 = handoverRepo.save(ShiftHandover.builder()
                .communityId("COMM-001")
                .outgoingGuardId(rajan.getId()).outgoingGuardName("Rajan Kumar")
                .incomingGuardId(sunil.getId()).incomingGuardName("Sunil Verma")
                .gateId("GATE-MAIN").gateName("Main Gate")
                .handoverInitiatedAt(now.minusHours(6))
                .handoverCompletedAt(now.minusHours(6).plusMinutes(12))
                .visitorsStillInside(14).uncollectedPackages(2).openIncidents(1)
                .pendingDeliveries(3).brokenEquipmentCount(1)
                .openItemsSummary("14 visitors, 2 packages, 1 incident (broken boom barrier), 3 deliveries, 1 equipment")
                .outgoingAcknowledged(true).incomingAcknowledged(true).supervisorVerified(true)
                .supervisorName("Manoj Tiwari")
                .outgoingBiometricVerified(true).incomingBiometricVerified(true)
                .status(ShiftHandover.HandoverStatus.COMPLETED)
                .remarks("Smooth handover. Boom barrier repair team arriving at 10 PM.").build());

        ShiftHandover h2 = handoverRepo.save(ShiftHandover.builder()
                .communityId("COMM-001")
                .outgoingGuardId(deepak.getId()).outgoingGuardName("Deepak Singh")
                .incomingGuardId(anil.getId()).incomingGuardName("Anil Yadav")
                .gateId("GATE-SIDE").gateName("Side Gate")
                .handoverInitiatedAt(now.minusMinutes(30))
                .visitorsStillInside(3).uncollectedPackages(0).openIncidents(0)
                .pendingDeliveries(1).brokenEquipmentCount(0)
                .openItemsSummary("3 visitors, 1 pending delivery")
                .outgoingAcknowledged(true).incomingAcknowledged(false)
                .outgoingBiometricVerified(true).incomingBiometricVerified(false)
                .status(ShiftHandover.HandoverStatus.INCOMING_REVIEW).build());

        // Checklist items for handover 1
        checklistRepo.saveAll(List.of(
                HandoverChecklistItem.builder().handoverId(h1.getId())
                        .category(HandoverChecklistItem.ChecklistCategory.VISITORS_INSIDE)
                        .itemDescription("14 visitors still inside: 8 in Tower A (party), 4 domestic help, 2 delivery agents")
                        .count(14).priority(HandoverChecklistItem.ItemPriority.HIGH)
                        .itemStatus(HandoverChecklistItem.ItemStatus.ACKNOWLEDGED)
                        .outgoingConfirmed(true).incomingAcknowledged(true)
                        .actionRequired("Track exit of party guests by 11 PM").build(),
                HandoverChecklistItem.builder().handoverId(h1.getId())
                        .category(HandoverChecklistItem.ChecklistCategory.UNCOLLECTED_PACKAGES)
                        .itemDescription("2 Amazon packages for A-305 and B-102, resident not responding")
                        .count(2).priority(HandoverChecklistItem.ItemPriority.MEDIUM)
                        .itemStatus(HandoverChecklistItem.ItemStatus.CARRIED_OVER)
                        .outgoingConfirmed(true).incomingAcknowledged(true)
                        .actionRequired("Retry delivery notification at 9 PM").build(),
                HandoverChecklistItem.builder().handoverId(h1.getId())
                        .category(HandoverChecklistItem.ChecklistCategory.OPEN_INCIDENTS)
                        .itemDescription("Main gate boom barrier motor overheating — manually operated since 3 PM")
                        .count(1).priority(HandoverChecklistItem.ItemPriority.CRITICAL)
                        .itemStatus(HandoverChecklistItem.ItemStatus.OPEN)
                        .outgoingConfirmed(true).incomingAcknowledged(true)
                        .actionRequired("Repair team confirmed for 10 PM arrival. Manual barrier ops until fixed.").build(),
                HandoverChecklistItem.builder().handoverId(h1.getId())
                        .category(HandoverChecklistItem.ChecklistCategory.EQUIPMENT_STATUS)
                        .itemDescription("CCTV Camera #7 (parking area B) offline since 2 PM")
                        .count(1).priority(HandoverChecklistItem.ItemPriority.CRITICAL)
                        .itemStatus(HandoverChecklistItem.ItemStatus.OPEN)
                        .outgoingConfirmed(true).incomingAcknowledged(true)
                        .actionRequired("IT support ticket #4521 raised. Extra patrol of parking B every 30 mins.").build()
        ));

        // Checklist items for handover 2
        checklistRepo.saveAll(List.of(
                HandoverChecklistItem.builder().handoverId(h2.getId())
                        .category(HandoverChecklistItem.ChecklistCategory.VISITORS_INSIDE)
                        .itemDescription("3 visitors: 2 domestic help (expected exit 7 PM), 1 guest in C-401")
                        .count(3).priority(HandoverChecklistItem.ItemPriority.MEDIUM)
                        .itemStatus(HandoverChecklistItem.ItemStatus.OPEN)
                        .outgoingConfirmed(true).incomingAcknowledged(false)
                        .actionRequired("Domestic help should exit before 7:30 PM").build(),
                HandoverChecklistItem.builder().handoverId(h2.getId())
                        .category(HandoverChecklistItem.ChecklistCategory.PENDING_DELIVERIES)
                        .itemDescription("Flipkart delivery for D-205 — resident requested hold at gate")
                        .count(1).priority(HandoverChecklistItem.ItemPriority.LOW)
                        .itemStatus(HandoverChecklistItem.ItemStatus.OPEN)
                        .outgoingConfirmed(true).incomingAcknowledged(false)
                        .actionRequired("Resident will collect by 8 PM").build()
        ));

        // --- Delta Sync Cursors ---
        String[] entities = {"RESIDENTS", "VEHICLES", "DAILY_HELP", "VISITORS_EXPECTED", "PACKAGES_PENDING", "INTERCOM_DIRECTORY", "EMERGENCY_CONTACTS"};
        long[] records = {12000, 8000, 3000, 450, 120, 12000, 500};
        long[] tombstones = {230, 180, 450, 1200, 800, 50, 10};
        long[] bytes = {850000, 620000, 280000, 45000, 12000, 380000, 25000};
        long[] cache = {280, 190, 85, 12, 3, 150, 8};
        double[] fprs = {0.008, 0.009, 0.007, 0.012, 0.015, 0.008, 0.02};

        for (int d = 1; d <= 3; d++) {
            for (int e = 0; e < entities.length; e++) {
                SyncStatus status = d == 3 && e == 2 ? SyncStatus.STALE :
                        d == 2 && e == 4 ? SyncStatus.ERROR : SyncStatus.UP_TO_DATE;
                cursorRepo.save(DeltaSyncCursor.builder()
                        .deviceId("TABLET-GATE-" + d).guardId("GRD-00" + d).communityId("COMM-001")
                        .syncEntity(SyncEntity.valueOf(entities[e]))
                        .lastCursor(now.minusMinutes(d * 15L + e).toString() + ":seq-" + (1000 + e * 100))
                        .lastSyncAt(now.minusMinutes(d * 15L + e))
                        .recordsSynced(records[e]).tombstonesPurged(tombstones[e])
                        .bytesTransferred(bytes[e] / d).bloomFilterSizeBytes(records[e] * 10 / 8)
                        .bloomFilterFalsePositiveRate(fprs[e])
                        .totalDeviceStorageMb(16384).usedCacheMb(cache[e])
                        .evictedRecords(d == 1 ? 0 : tombstones[e] / 2)
                        .status(status).build());
            }
        }

        // --- Gamification Events ---
        gamificationRepo.saveAll(List.of(
                GamificationEvent.builder().guardId(rajan.getId()).guardName("Rajan Kumar").communityId("COMM-001")
                        .achievementType(AchievementType.MONTHLY_ATTENDANCE_100).pointsAwarded(500)
                        .description("100% attendance for November 2026").earnedAt(now.minusDays(1))
                        .badgeLevel(BadgeLevel.GOLD_BADGE).badgeName("Perfect Month").notified(true).build(),
                GamificationEvent.builder().guardId(rajan.getId()).guardName("Rajan Kumar").communityId("COMM-001")
                        .achievementType(AchievementType.ON_TIME_CLOCK_IN).pointsAwarded(10)
                        .description("On-time clock-in streak: 30 days").earnedAt(now.minusHours(6))
                        .badgeLevel(BadgeLevel.NONE).notified(true).build(),
                GamificationEvent.builder().guardId(manoj.getId()).guardName("Manoj Tiwari").communityId("COMM-001")
                        .achievementType(AchievementType.EMERGENCY_RESPONSE).pointsAwarded(200)
                        .description("Handled gas leak emergency: evacuated 20 residents in 8 minutes").earnedAt(now.minusDays(5))
                        .badgeLevel(BadgeLevel.DIAMOND_BADGE).badgeName("Emergency Hero").notified(true).build(),
                GamificationEvent.builder().guardId(sunil.getId()).guardName("Sunil Verma").communityId("COMM-001")
                        .achievementType(AchievementType.FAST_VISITOR_PROCESSING).pointsAwarded(25)
                        .description("Processed 50 visitors in under 2 minutes avg").earnedAt(now.minusDays(2))
                        .badgeLevel(BadgeLevel.SILVER_BADGE).badgeName("Speed Gate").notified(true).build(),
                GamificationEvent.builder().guardId(deepak.getId()).guardName("Deepak Singh").communityId("COMM-001")
                        .achievementType(AchievementType.PACKAGE_DELIVERY_STREAK).pointsAwarded(30)
                        .description("Zero lost packages for 90 days").earnedAt(now.minusDays(3))
                        .badgeLevel(BadgeLevel.BRONZE_BADGE).badgeName("Package Pro").notified(true).build(),
                GamificationEvent.builder().guardId(manoj.getId()).guardName("Manoj Tiwari").communityId("COMM-001")
                        .achievementType(AchievementType.TRAINING_COMPLETED).pointsAwarded(100)
                        .description("Completed Fire Safety & First Aid certification").earnedAt(now.minusDays(10))
                        .badgeLevel(BadgeLevel.GOLD_BADGE).badgeName("Safety Expert").notified(true).build()
        ));

        Map<String, Object> result = new HashMap<>();
        result.put("message", "Guard ops demo data seeded");
        result.put("guards", 5);
        result.put("shiftRosters", 5);
        result.put("handovers", 2);
        result.put("checklistItems", 6);
        result.put("syncCursors", 21);
        result.put("gamificationEvents", 6);
        return ResponseEntity.ok(result);
    }
}

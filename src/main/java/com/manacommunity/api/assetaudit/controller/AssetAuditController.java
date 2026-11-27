package com.manacommunity.api.assetaudit.controller;

import com.manacommunity.api.assetaudit.entity.AssetInspection;
import com.manacommunity.api.assetaudit.entity.AssetInspection.InspectionResult;
import com.manacommunity.api.assetaudit.entity.AssetInspection.InspectionType;
import com.manacommunity.api.assetaudit.entity.HousekeepingTask;
import com.manacommunity.api.assetaudit.entity.SocietyAsset;
import com.manacommunity.api.assetaudit.entity.SocietyAsset.*;
import com.manacommunity.api.assetaudit.repository.AssetInspectionRepository;
import com.manacommunity.api.assetaudit.repository.HousekeepingTaskRepository;
import com.manacommunity.api.assetaudit.repository.SocietyAssetRepository;
import com.manacommunity.api.assetaudit.service.AssetLifecycleService;
import com.manacommunity.api.assetaudit.service.HousekeepingWorkflowService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/asset-audit")
@RequiredArgsConstructor
public class AssetAuditController {

    private final AssetLifecycleService assetService;
    private final HousekeepingWorkflowService housekeepingService;
    private final SocietyAssetRepository assetRepo;
    private final AssetInspectionRepository inspectionRepo;
    private final HousekeepingTaskRepository taskRepo;

    @GetMapping("/assets/dashboard")
    public ResponseEntity<?> getAssetDashboard() {
        return ResponseEntity.ok(assetService.getDashboardSummary(1L));
    }

    @GetMapping("/assets/scan/{qrCode}")
    public ResponseEntity<?> scanAssetQr(@PathVariable String qrCode) {
        return ResponseEntity.ok(assetService.scanQrAndGetAsset(qrCode));
    }

    @PostMapping("/assets/inspect")
    public ResponseEntity<?> recordInspection(@RequestBody Map<String, Object> req) {
        AssetInspection inspection = assetService.recordInspection(
                (String) req.get("qrCode"),
                (String) req.get("technicianId"),
                (String) req.get("technicianName"),
                InspectionType.valueOf((String) req.get("inspectionType")),
                InspectionResult.valueOf((String) req.get("result")),
                ((Number) req.getOrDefault("pressure", 0)).doubleValue(),
                ((Number) req.getOrDefault("temperature", 0)).doubleValue(),
                ((Number) req.getOrDefault("voltage", 0)).doubleValue(),
                ((Number) req.getOrDefault("runHours", 0)).intValue(),
                (String) req.getOrDefault("partsReplaced", "[]"),
                (String) req.getOrDefault("remarks", ""),
                Boolean.TRUE.equals(req.get("tamperDetected"))
        );
        return ResponseEntity.ok(inspection);
    }

    @GetMapping("/assets/{assetId}/inspections")
    public ResponseEntity<?> getInspectionHistory(@PathVariable Long assetId) {
        return ResponseEntity.ok(assetService.getInspectionHistory(assetId));
    }

    @GetMapping("/assets/{assetId}/depreciation")
    public ResponseEntity<?> getDepreciation(@PathVariable Long assetId) {
        SocietyAsset asset = assetRepo.findById(assetId)
                .orElseThrow(() -> new RuntimeException("Asset not found"));
        return ResponseEntity.ok(assetService.calculateDepreciation(asset));
    }

    @GetMapping("/housekeeping/dashboard")
    public ResponseEntity<?> getHousekeepingDashboard() {
        return ResponseEntity.ok(housekeepingService.getDashboard(1L));
    }

    @PostMapping("/housekeeping/report-dirty")
    public ResponseEntity<?> reportDirty(@RequestBody Map<String, String> req) {
        return ResponseEntity.ok(housekeepingService.reportDirtyLocation(
                req.get("qrCode"), req.get("remarks")));
    }

    @PostMapping("/housekeeping/{taskId}/assign")
    public ResponseEntity<?> assignTask(@PathVariable Long taskId, @RequestBody Map<String, String> req) {
        return ResponseEntity.ok(housekeepingService.assignTask(taskId, req.get("staffId"), req.get("staffName")));
    }

    @PostMapping("/housekeeping/{taskId}/start")
    public ResponseEntity<?> startTask(@PathVariable Long taskId) {
        return ResponseEntity.ok(housekeepingService.startTask(taskId));
    }

    @PostMapping("/housekeeping/{taskId}/complete")
    public ResponseEntity<?> completeTask(@PathVariable Long taskId, @RequestBody Map<String, String> req) {
        return ResponseEntity.ok(housekeepingService.completeTask(taskId,
                req.getOrDefault("completedItems", "[]"), req.get("photoAfterUrl")));
    }

    @PostMapping("/housekeeping/escalate")
    public ResponseEntity<?> runEscalation() {
        return ResponseEntity.ok(housekeepingService.checkAndEscalate());
    }

    @GetMapping("/housekeeping/checklist/{locationType}")
    public ResponseEntity<?> getChecklist(@PathVariable String locationType) {
        return ResponseEntity.ok(housekeepingService.generateChecklist(
                HousekeepingTask.LocationType.valueOf(locationType)));
    }

    @PostConstruct
    public void seedDemoData() {
        if (assetRepo.count() > 0) return;

        // Seed assets
        SocietyAsset fireExt = SocietyAsset.builder()
                .communityId(1L).name("Fire Extinguisher A-101").assetTag("FE-A101")
                .qrCode("ASSET-FE-A101").category(AssetCategory.FIRE_EXTINGUISHER)
                .towerName("Tower A").floor("1").location("Near elevator lobby")
                .make("Cease Fire").modelNumber("CF-ABC-5KG")
                .purchaseDate(LocalDate.of(2022, 3, 15)).purchaseCost(4500.0)
                .warrantyEndDate(LocalDate.of(2027, 3, 15))
                .depreciationMethod(DepreciationMethod.STRAIGHT_LINE)
                .usefulLifeYears(10).annualDepreciationRate(10.0).currentBookValue(2700.0)
                .status(AssetStatus.ACTIVE).complianceStatus(ComplianceStatus.COMPLIANT)
                .inspectionIntervalDays(180)
                .lastInspectionDate(LocalDate.now().minusDays(170))
                .nextInspectionDue(LocalDate.now().plusDays(10))
                .totalInspections(3).overdueInspections(0)
                .build();

        SocietyAsset stpPump = SocietyAsset.builder()
                .communityId(1L).name("STP Pump Unit-1").assetTag("STP-001")
                .qrCode("ASSET-STP-001").category(AssetCategory.STP_PUMP)
                .towerName("Common Area").floor("-1").location("Basement STP room")
                .make("Grundfos").modelNumber("CR 32-2")
                .purchaseDate(LocalDate.of(2020, 6, 1)).purchaseCost(285000.0)
                .warrantyEndDate(LocalDate.of(2025, 6, 1))
                .depreciationMethod(DepreciationMethod.WRITTEN_DOWN_VALUE)
                .usefulLifeYears(15).annualDepreciationRate(15.0).currentBookValue(118500.0)
                .status(AssetStatus.ACTIVE).complianceStatus(ComplianceStatus.DUE_SOON)
                .inspectionIntervalDays(30)
                .lastInspectionDate(LocalDate.now().minusDays(25))
                .nextInspectionDue(LocalDate.now().plusDays(5))
                .totalInspections(22).overdueInspections(1)
                .build();

        SocietyAsset gymTreadmill = SocietyAsset.builder()
                .communityId(1L).name("Treadmill Pro-3000").assetTag("GYM-TR-003")
                .qrCode("ASSET-GYM-TR003").category(AssetCategory.GYM_EQUIPMENT)
                .towerName("Clubhouse").floor("0").location("Gymnasium")
                .make("Technogym").modelNumber("Excite Run 1000")
                .purchaseDate(LocalDate.of(2023, 1, 10)).purchaseCost(175000.0)
                .warrantyEndDate(LocalDate.of(2026, 1, 10))
                .depreciationMethod(DepreciationMethod.STRAIGHT_LINE)
                .usefulLifeYears(7).annualDepreciationRate(14.3).currentBookValue(100000.0)
                .status(AssetStatus.ACTIVE).complianceStatus(ComplianceStatus.COMPLIANT)
                .inspectionIntervalDays(90)
                .lastInspectionDate(LocalDate.now().minusDays(45))
                .nextInspectionDue(LocalDate.now().plusDays(45))
                .totalInspections(8).overdueInspections(0)
                .build();

        SocietyAsset transformer = SocietyAsset.builder()
                .communityId(1L).name("HT Transformer 500KVA").assetTag("ELEC-TR-01")
                .qrCode("ASSET-ELEC-TR01").category(AssetCategory.TRANSFORMER)
                .towerName("Common Area").floor("-1").location("Electrical room")
                .make("Schneider Electric").modelNumber("Trihal 500")
                .purchaseDate(LocalDate.of(2019, 8, 20)).purchaseCost(1200000.0)
                .warrantyEndDate(LocalDate.of(2024, 8, 20))
                .depreciationMethod(DepreciationMethod.STRAIGHT_LINE)
                .usefulLifeYears(20).annualDepreciationRate(5.0).currentBookValue(840000.0)
                .status(AssetStatus.ACTIVE).complianceStatus(ComplianceStatus.OVERDUE)
                .inspectionIntervalDays(60)
                .lastInspectionDate(LocalDate.now().minusDays(75))
                .nextInspectionDue(LocalDate.now().minusDays(15))
                .totalInspections(15).overdueInspections(2)
                .build();

        SocietyAsset elevator = SocietyAsset.builder()
                .communityId(1L).name("Passenger Elevator A-1").assetTag("LIFT-A-01")
                .qrCode("ASSET-LIFT-A01").category(AssetCategory.ELEVATOR)
                .towerName("Tower A").floor("0").location("Main lobby elevator")
                .make("KONE").modelNumber("MonoSpace 500")
                .purchaseDate(LocalDate.of(2019, 3, 1)).purchaseCost(2500000.0)
                .warrantyEndDate(LocalDate.of(2029, 3, 1))
                .depreciationMethod(DepreciationMethod.STRAIGHT_LINE)
                .usefulLifeYears(25).annualDepreciationRate(4.0).currentBookValue(1750000.0)
                .status(AssetStatus.ACTIVE).complianceStatus(ComplianceStatus.COMPLIANT)
                .inspectionIntervalDays(30)
                .lastInspectionDate(LocalDate.now().minusDays(20))
                .nextInspectionDue(LocalDate.now().plusDays(10))
                .totalInspections(28).overdueInspections(0)
                .build();

        assetRepo.saveAll(List.of(fireExt, stpPump, gymTreadmill, transformer, elevator));

        // Seed inspections
        inspectionRepo.saveAll(List.of(
                AssetInspection.builder()
                        .assetId(fireExt.getId()).qrCodeScanned("ASSET-FE-A101")
                        .technicianId("TECH-001").technicianName("Rajesh Kumar")
                        .inspectionType(InspectionType.SAFETY_COMPLIANCE)
                        .result(InspectionResult.PASS)
                        .pressureReading(14.5).temperatureReading(28.0).voltageReading(0)
                        .runHours(0).qrMatchVerified(true).tamperDetected(false)
                        .inspectedAt(LocalDateTime.now().minusDays(170)).inspectionDurationSec(300L)
                        .build(),
                AssetInspection.builder()
                        .assetId(stpPump.getId()).qrCodeScanned("ASSET-STP-001")
                        .technicianId("TECH-002").technicianName("Suresh Patil")
                        .inspectionType(InspectionType.ROUTINE)
                        .result(InspectionResult.NEEDS_REPAIR)
                        .pressureReading(3.2).temperatureReading(45.0).voltageReading(415.0)
                        .runHours(12500).partsReplacedJson("[\"Mechanical seal\"]")
                        .remarks("Vibration slightly elevated â€” monitor next cycle")
                        .qrMatchVerified(true).tamperDetected(false)
                        .inspectedAt(LocalDateTime.now().minusDays(25)).inspectionDurationSec(900L)
                        .build(),
                AssetInspection.builder()
                        .assetId(transformer.getId()).qrCodeScanned("ASSET-ELEC-TR01")
                        .technicianId("TECH-003").technicianName("Vikram Singh")
                        .inspectionType(InspectionType.SAFETY_COMPLIANCE)
                        .result(InspectionResult.CRITICAL)
                        .pressureReading(0).temperatureReading(72.0).voltageReading(11000.0)
                        .runHours(45000).remarks("Oil temperature exceeding threshold â€” immediate cooling required")
                        .qrMatchVerified(true).tamperDetected(true)
                        .inspectedAt(LocalDateTime.now().minusDays(75)).inspectionDurationSec(1800L)
                        .build()
        ));

        // Seed housekeeping tasks
        taskRepo.saveAll(List.of(
                HousekeepingTask.builder()
                        .communityId(1L).qrCode("LOBBY-A-GF").locationId("LOC-LOBBY-A-GF")
                        .locationName("Tower A Ground Floor Lobby").locationType(HousekeepingTask.LocationType.TOWER_LOBBY)
                        .towerName("Tower A").floor("0")
                        .status(HousekeepingTask.TaskStatus.COMPLETED).priority(HousekeepingTask.TaskPriority.NORMAL)
                        .scheduledTime(LocalDateTime.now().minusHours(3).toLocalTime())
                        .assignedStaffId("HK-001").assignedStaffName("Lakshmi Devi")
                        .checklistJson("[\"Sweep and mop floor\",\"Wipe elevator buttons\",\"Clean glass panels\",\"Empty trash bins\"]")
                        .completedItemsJson("[\"Sweep and mop floor\",\"Wipe elevator buttons\",\"Clean glass panels\",\"Empty trash bins\"]")
                        .startedAt(LocalDateTime.now().minusHours(3))
                        .completedAt(LocalDateTime.now().minusHours(2).minusMinutes(30))
                        .durationMinutes(30).ratingScore(4.5)
                        .residentReported(false).escalated(false).escalationLevel(0)
                        .build(),
                HousekeepingTask.builder()
                        .communityId(1L).qrCode("REST-CLB-01").locationId("LOC-REST-CLB-01")
                        .locationName("Clubhouse Restroom 1").locationType(HousekeepingTask.LocationType.CLUBHOUSE_RESTROOM)
                        .towerName("Clubhouse").floor("0")
                        .status(HousekeepingTask.TaskStatus.REPORTED_DIRTY).priority(HousekeepingTask.TaskPriority.URGENT)
                        .assignedStaffId("HK-002").assignedStaffName("Meena Kumari")
                        .checklistJson("[\"Scrub toilets\",\"Clean wash basins\",\"Mop floor\",\"Refill dispensers\"]")
                        .residentReported(true).residentReportQrScan("REST-CLB-01")
                        .residentReportedAt(LocalDateTime.now().minusMinutes(10))
                        .residentRemarks("Floor is wet and dirty, soap dispenser empty")
                        .escalated(false).escalationLevel(0)
                        .build(),
                HousekeepingTask.builder()
                        .communityId(1L).qrCode("LOBBY-B-3F").locationId("LOC-LOBBY-B-3F")
                        .locationName("Tower B 3rd Floor Lobby").locationType(HousekeepingTask.LocationType.TOWER_LOBBY)
                        .towerName("Tower B").floor("3")
                        .status(HousekeepingTask.TaskStatus.IN_PROGRESS).priority(HousekeepingTask.TaskPriority.NORMAL)
                        .scheduledTime(LocalDateTime.now().minusMinutes(45).toLocalTime())
                        .assignedStaffId("HK-003").assignedStaffName("Ramu Prasad")
                        .checklistJson("[\"Sweep and mop floor\",\"Wipe elevator buttons\",\"Clean glass panels\",\"Empty trash bins\",\"Check light fixtures\"]")
                        .startedAt(LocalDateTime.now().minusMinutes(20))
                        .residentReported(false).escalated(false).escalationLevel(0)
                        .build(),
                HousekeepingTask.builder()
                        .communityId(1L).qrCode("GYM-CLB").locationId("LOC-GYM-CLB")
                        .locationName("Clubhouse Gymnasium").locationType(HousekeepingTask.LocationType.GYM)
                        .towerName("Clubhouse").floor("0")
                        .status(HousekeepingTask.TaskStatus.ESCALATED).priority(HousekeepingTask.TaskPriority.HIGH)
                        .scheduledTime(LocalDateTime.now().minusHours(1).toLocalTime())
                        .assignedStaffId("HK-002").assignedStaffName("Meena Kumari")
                        .checklistJson("[\"Wipe equipment\",\"Mop rubber flooring\",\"Clean mirrors\",\"Check water cooler\"]")
                        .residentReported(true).residentReportQrScan("GYM-CLB")
                        .residentReportedAt(LocalDateTime.now().minusMinutes(45))
                        .residentRemarks("Equipment not wiped after morning session, water cooler empty")
                        .escalated(true).escalationLevel(1)
                        .escalationReason("Unresolved for 30 min â€” escalated to Supervisor")
                        .escalatedAt(LocalDateTime.now().minusMinutes(15))
                        .build()
        ));
    }
}

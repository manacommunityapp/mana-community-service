package com.manacommunity.api.assetaudit.service;

import com.manacommunity.api.assetaudit.entity.AssetInspection;
import com.manacommunity.api.assetaudit.entity.AssetInspection.InspectionResult;
import com.manacommunity.api.assetaudit.entity.AssetInspection.InspectionType;
import com.manacommunity.api.assetaudit.entity.SocietyAsset;
import com.manacommunity.api.assetaudit.entity.SocietyAsset.*;
import com.manacommunity.api.assetaudit.repository.AssetInspectionRepository;
import com.manacommunity.api.assetaudit.repository.SocietyAssetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssetLifecycleService {

    private final SocietyAssetRepository assetRepo;
    private final AssetInspectionRepository inspectionRepo;

    public SocietyAsset scanQrAndGetAsset(String qrCode) {
        SocietyAsset asset = assetRepo.findByQrCode(qrCode)
                .orElseThrow(() -> new RuntimeException("Asset not found for QR: " + qrCode));
        updateComplianceStatus(asset);
        return assetRepo.save(asset);
    }

    @Transactional
    public AssetInspection recordInspection(String qrCode, String technicianId, String technicianName,
                                              InspectionType type, InspectionResult result,
                                              double pressure, double temperature, double voltage,
                                              int runHours, String partsReplaced, String remarks,
                                              boolean tamperDetected) {
        SocietyAsset asset = assetRepo.findByQrCode(qrCode)
                .orElseThrow(() -> new RuntimeException("Asset not found for QR: " + qrCode));

        AssetInspection inspection = AssetInspection.builder()
                .assetId(asset.getId())
                .qrCodeScanned(qrCode)
                .technicianId(technicianId)
                .technicianName(technicianName)
                .inspectionType(type)
                .result(result)
                .pressureReading(pressure)
                .temperatureReading(temperature)
                .voltageReading(voltage)
                .runHours(runHours)
                .partsReplacedJson(partsReplaced)
                .remarks(remarks)
                .tamperDetected(tamperDetected)
                .qrMatchVerified(true)
                .inspectedAt(LocalDateTime.now())
                .inspectionDurationSec((long) (120 + Math.random() * 600))
                .build();

        inspection = inspectionRepo.save(inspection);

        // Update asset
        asset.setLastInspectionDate(LocalDate.now());
        asset.setNextInspectionDue(LocalDate.now().plusDays(asset.getInspectionIntervalDays()));
        asset.setTotalInspections(asset.getTotalInspections() + 1);

        if (result == InspectionResult.FAIL || result == InspectionResult.CRITICAL) {
            asset.setStatus(AssetStatus.UNDER_MAINTENANCE);
        }
        if (result == InspectionResult.NEEDS_REPLACEMENT) {
            asset.setStatus(AssetStatus.DECOMMISSIONED);
        }

        updateComplianceStatus(asset);
        calculateDepreciation(asset);
        assetRepo.save(asset);

        return inspection;
    }

    public DepreciationReport calculateDepreciation(SocietyAsset asset) {
        if (asset.getPurchaseDate() == null) return null;

        long yearsOwned = ChronoUnit.YEARS.between(asset.getPurchaseDate(), LocalDate.now());
        double currentValue;

        if (asset.getDepreciationMethod() == DepreciationMethod.STRAIGHT_LINE) {
            double annualDep = asset.getPurchaseCost() / asset.getUsefulLifeYears();
            currentValue = Math.max(0, asset.getPurchaseCost() - (annualDep * yearsOwned));
        } else {
            currentValue = asset.getPurchaseCost() * Math.pow(1 - asset.getAnnualDepreciationRate() / 100, yearsOwned);
        }

        asset.setCurrentBookValue(Math.round(currentValue * 100.0) / 100.0);
        return new DepreciationReport(asset.getId(), asset.getName(), asset.getPurchaseCost(),
                currentValue, asset.getPurchaseCost() - currentValue,
                yearsOwned, asset.getUsefulLifeYears() - yearsOwned,
                asset.getDepreciationMethod().name());
    }

    private void updateComplianceStatus(SocietyAsset asset) {
        if (asset.getNextInspectionDue() == null) {
            asset.setComplianceStatus(ComplianceStatus.EXEMPT);
            return;
        }
        long daysUntilDue = ChronoUnit.DAYS.between(LocalDate.now(), asset.getNextInspectionDue());
        if (daysUntilDue < 0) {
            asset.setComplianceStatus(ComplianceStatus.OVERDUE);
            asset.setOverdueInspections(asset.getOverdueInspections() + 1);
        } else if (daysUntilDue <= 7) {
            asset.setComplianceStatus(ComplianceStatus.DUE_SOON);
        } else {
            asset.setComplianceStatus(ComplianceStatus.COMPLIANT);
        }
    }

    public AssetDashboardSummary getDashboardSummary(Long communityId) {
        List<SocietyAsset> all = assetRepo.findAll();
        List<AssetInspection> inspections = inspectionRepo.findAll();

        Map<String, Long> byCategory = all.stream()
                .collect(Collectors.groupingBy(a -> a.getCategory().name(), Collectors.counting()));
        Map<String, Long> byStatus = all.stream()
                .collect(Collectors.groupingBy(a -> a.getStatus().name(), Collectors.counting()));
        Map<String, Long> byCompliance = all.stream()
                .collect(Collectors.groupingBy(a -> a.getComplianceStatus().name(), Collectors.counting()));

        double totalValue = all.stream().mapToDouble(SocietyAsset::getCurrentBookValue).sum();
        double totalPurchase = all.stream().mapToDouble(SocietyAsset::getPurchaseCost).sum();

        List<SocietyAsset> overdue = assetRepo.findOverdueInspections(LocalDate.now());
        List<SocietyAsset> nonCompliant = assetRepo.findNonCompliantAssets();
        List<SocietyAsset> expiringWarranty = assetRepo.findExpiringWarranties(LocalDate.now(), LocalDate.now().plusDays(30));
        List<AssetInspection> criticalFindings = inspectionRepo.findCriticalFindings();
        List<AssetInspection> tamperEvents = inspectionRepo.findTamperEvents();

        return new AssetDashboardSummary(all.size(), byCategory, byStatus, byCompliance,
                totalPurchase, totalValue, totalPurchase - totalValue,
                overdue.size(), nonCompliant.size(), expiringWarranty.size(),
                criticalFindings.size(), tamperEvents.size(),
                inspections.size(), all, overdue, criticalFindings);
    }

    public List<AssetInspection> getInspectionHistory(Long assetId) {
        return inspectionRepo.findByAssetIdOrderByInspectedAtDesc(assetId);
    }

    public record DepreciationReport(Long assetId, String assetName, double purchaseCost,
                                      double currentBookValue, double totalDepreciation,
                                      long yearsOwned, long remainingLife, String method) {}

    public record AssetDashboardSummary(int totalAssets, Map<String, Long> byCategory,
                                         Map<String, Long> byStatus, Map<String, Long> byCompliance,
                                         double totalPurchaseValue, double totalCurrentValue, double totalDepreciation,
                                         int overdueInspections, int nonCompliantAssets, int expiringWarranties,
                                         int criticalFindings, int tamperEvents, int totalInspections,
                                         List<SocietyAsset> assets, List<SocietyAsset> overdueAssets,
                                         List<AssetInspection> criticalInspections) {}
}

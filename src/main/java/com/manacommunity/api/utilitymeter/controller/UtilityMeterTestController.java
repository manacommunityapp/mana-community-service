package com.manacommunity.api.utilitymeter.controller;

import com.manacommunity.api.utilitymeter.entity.*;
import com.manacommunity.api.utilitymeter.repository.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/utility-meter-test")
public class UtilityMeterTestController {

    private final SmartMeterTestRepository meterRepo;
    private final TankerPatrolTestRepository patrolRepo;

    public UtilityMeterTestController(SmartMeterTestRepository meterRepo, TankerPatrolTestRepository patrolRepo) {
        this.meterRepo = meterRepo;
        this.patrolRepo = patrolRepo;
    }

    public record UtilityDashboard(int totalTests, int passed, int failed, int pending, double passRate,
                                    List<SmartMeterTest> meterTests, List<TankerPatrolTest> patrolTests) {}

    @GetMapping("/dashboard")
    public ResponseEntity<UtilityDashboard> getDashboard() {
        var m = meterRepo.findAll(); var p = patrolRepo.findAll();
        int total = m.size() + p.size();
        if (total == 0) return seedDemo();
        long pass = m.stream().filter(t -> t.getTestResult() == SmartMeterTest.TestResult.PASS).count()
                + p.stream().filter(t -> t.getTestResult() == TankerPatrolTest.TestResult.PASS).count();
        long fail = total - pass;
        return ResponseEntity.ok(new UtilityDashboard(total, (int) pass, (int) fail, 0, Math.round(pass * 1000.0 / total) / 10.0, m, p));
    }

    @PostMapping("/seed-demo")
    public ResponseEntity<UtilityDashboard> seedDemo() {
        var m1 = SmartMeterTest.builder().communityId("COMM-001").testCaseCode("MTR-001").testCaseName("Out-of-order 15-min readings reordered")
                .scenario(SmartMeterTest.MeterScenario.OUT_OF_ORDER_READINGS).meterType(SmartMeterTest.MeterType.ELECTRICITY)
                .flatNumber("A-301").currentBalance(1250.0).autoCutoffThreshold(0).autoCutoffTriggered(false)
                .readingIntervalMinutes(15).outOfOrderReadings(3).readingsReordered(true)
                .tariffSource(SmartMeterTest.TariffSource.GRID).tariffRatePerUnit(8.5).dgTariffRatePerUnit(14.0).tariffSwitchCorrect(true)
                .testResult(SmartMeterTest.TestResult.PASS).failureReason("").build();

        var m2 = SmartMeterTest.builder().communityId("COMM-001").testCaseCode("MTR-002").testCaseName("Negative balance — auto-cutoff not triggered")
                .scenario(SmartMeterTest.MeterScenario.NEGATIVE_BALANCE_AUTO_CUTOFF).meterType(SmartMeterTest.MeterType.ELECTRICITY)
                .flatNumber("B-105").currentBalance(-45.0).autoCutoffThreshold(0).autoCutoffTriggered(false)
                .readingIntervalMinutes(15).outOfOrderReadings(0).readingsReordered(false)
                .tariffSource(SmartMeterTest.TariffSource.GRID).tariffRatePerUnit(8.5).dgTariffRatePerUnit(14.0).tariffSwitchCorrect(true)
                .testResult(SmartMeterTest.TestResult.FAIL).failureReason("Balance ₹-45 but meter not cut off — relay command not sent to smart meter").build();

        var m3 = SmartMeterTest.builder().communityId("COMM-001").testCaseCode("MTR-003").testCaseName("DG → Grid tariff switch at power restore")
                .scenario(SmartMeterTest.MeterScenario.DG_TO_GRID_TARIFF_SWITCH).meterType(SmartMeterTest.MeterType.ELECTRICITY)
                .flatNumber("C-201").currentBalance(800.0).autoCutoffThreshold(0).autoCutoffTriggered(false)
                .readingIntervalMinutes(15).outOfOrderReadings(0).readingsReordered(false)
                .tariffSource(SmartMeterTest.TariffSource.DG_GENERATOR).tariffRatePerUnit(14.0).dgTariffRatePerUnit(14.0).tariffSwitchCorrect(true)
                .testResult(SmartMeterTest.TestResult.PASS).failureReason("").build();

        var m4 = SmartMeterTest.builder().communityId("COMM-001").testCaseCode("MTR-004").testCaseName("Water meter stale data — offline 2 hours")
                .scenario(SmartMeterTest.MeterScenario.METER_OFFLINE_STALE_DATA).meterType(SmartMeterTest.MeterType.WATER)
                .flatNumber("A-102").currentBalance(500.0).autoCutoffThreshold(0).autoCutoffTriggered(false)
                .readingIntervalMinutes(15).outOfOrderReadings(0).readingsReordered(false)
                .tariffSource(SmartMeterTest.TariffSource.GRID).tariffRatePerUnit(0.05).dgTariffRatePerUnit(0).tariffSwitchCorrect(true)
                .testResult(SmartMeterTest.TestResult.PASS).failureReason("").build();

        var p1 = TankerPatrolTest.builder().communityId("COMM-001").testCaseCode("PTL-001").testCaseName("Guard QR scan — valid GPS location")
                .scenario(TankerPatrolTest.PatrolScenario.QR_SCAN_VALID_LOCATION).verificationType(TankerPatrolTest.VerificationType.QR_CODE)
                .guardName("Ramesh").checkpointName("Gate 2 Booth").scannedLatitude(12.9716).scannedLongitude(77.5946)
                .expectedLatitude(12.9716).expectedLongitude(77.5946).gpsDeviationMeters(2.3).gpsSpoofDetected(false)
                .tankerVendor("").entryWeightKg(0).exitWeightKg(0).deliveredLiters(0).invoicedLiters(0).volumeVariancePercent(0)
                .testResult(TankerPatrolTest.TestResult.PASS).failureReason("").build();

        var p2 = TankerPatrolTest.builder().communityId("COMM-001").testCaseCode("PTL-002").testCaseName("NFC scan — GPS spoof from 5km away")
                .scenario(TankerPatrolTest.PatrolScenario.NFC_SCAN_GPS_SPOOF_REJECTED).verificationType(TankerPatrolTest.VerificationType.NFC)
                .guardName("Sunil").checkpointName("Basement Parking P2").scannedLatitude(13.0150).scannedLongitude(77.6100)
                .expectedLatitude(12.9716).expectedLongitude(77.5946).gpsDeviationMeters(5200).gpsSpoofDetected(true)
                .tankerVendor("").entryWeightKg(0).exitWeightKg(0).deliveredLiters(0).invoicedLiters(0).volumeVariancePercent(0)
                .testResult(TankerPatrolTest.TestResult.PASS).failureReason("").build();

        var p3 = TankerPatrolTest.builder().communityId("COMM-001").testCaseCode("PTL-003").testCaseName("Tanker 12,000L invoiced but 10,800L delivered")
                .scenario(TankerPatrolTest.PatrolScenario.TANKER_VOLUME_MISMATCH).verificationType(TankerPatrolTest.VerificationType.WEIGHT_BRIDGE)
                .guardName("").checkpointName("Main Gate Weighbridge").scannedLatitude(0).scannedLongitude(0)
                .expectedLatitude(0).expectedLongitude(0).gpsDeviationMeters(0).gpsSpoofDetected(false)
                .tankerVendor("AquaPure Tankers").entryWeightKg(24000).exitWeightKg(13200).deliveredLiters(10800).invoicedLiters(12000).volumeVariancePercent(10.0)
                .testResult(TankerPatrolTest.TestResult.FAIL).failureReason("1,200L (10%) volume shortfall — vendor invoiced 12,000L but weight-bridge shows 10,800L delivered").build();

        meterRepo.saveAll(List.of(m1, m2, m3, m4));
        patrolRepo.saveAll(List.of(p1, p2, p3));

        return ResponseEntity.ok(new UtilityDashboard(7, 5, 2, 0, 71.4,
                List.of(m1, m2, m3, m4), List.of(p1, p2, p3)));
    }
}

package com.manacommunity.api.megatownship.controller;

import com.manacommunity.api.megatownship.entity.*;
import com.manacommunity.api.megatownship.repository.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mega-township-test")
public class MegaTownshipTestController {

    private final FederatedGateTestRepository gateRepo;
    private final SharedAmenityTestRepository amenityRepo;
    private final ConsolidatedBillingTestRepository billingRepo;

    public MegaTownshipTestController(FederatedGateTestRepository gateRepo,
                                       SharedAmenityTestRepository amenityRepo,
                                       ConsolidatedBillingTestRepository billingRepo) {
        this.gateRepo = gateRepo;
        this.amenityRepo = amenityRepo;
        this.billingRepo = billingRepo;
    }

    public record MegaTownshipDashboard(int totalTests, int passed, int failed, int pending, double passRate,
                                         List<FederatedGateTest> gateTests, List<SharedAmenityTest> amenityTests,
                                         List<ConsolidatedBillingTest> billingTests) {}

    @GetMapping("/dashboard")
    public ResponseEntity<MegaTownshipDashboard> getDashboard() {
        var g = gateRepo.findAll(); var a = amenityRepo.findAll(); var b = billingRepo.findAll();
        int total = g.size() + a.size() + b.size();
        if (total == 0) return seedDemo();
        long pass = g.stream().filter(t -> t.getTestResult() == FederatedGateTest.TestResult.PASS).count()
                + a.stream().filter(t -> t.getTestResult() == SharedAmenityTest.TestResult.PASS).count()
                + b.stream().filter(t -> t.getTestResult() == ConsolidatedBillingTest.TestResult.PASS).count();
        return ResponseEntity.ok(new MegaTownshipDashboard(total, (int) pass, (int)(total - pass), 0, Math.round(pass * 1000.0 / total) / 10.0, g, a, b));
    }

    @PostMapping("/seed-demo")
    public ResponseEntity<MegaTownshipDashboard> seedDemo() {
        var g1 = FederatedGateTest.builder().communityId("MEGA-001").testCaseCode("GATE-001").testCaseName("Cross-tower visitor pass — Tower A to Tower C")
                .scenario(FederatedGateTest.GateScenario.CROSS_TOWER_VISITOR_PASS)
                .sourceTower("Tower A").destinationTower("Tower C").visitorType("Delivery")
                .crossTowerPassValid(true).centralGateSync(true).syncDelayMs(230)
                .rfidRecognizedAcrossTowers(false).anprCameraMatched(false).vehicleNumber("").boomBarrierOpened(true)
                .testResult(FederatedGateTest.TestResult.PASS).failureReason("").build();

        var g2 = FederatedGateTest.builder().communityId("MEGA-001").testCaseCode("GATE-002").testCaseName("ANPR vehicle parked in wrong tower lot")
                .scenario(FederatedGateTest.GateScenario.ANPR_VEHICLE_WRONG_TOWER_PARKING)
                .sourceTower("Tower B").destinationTower("Tower D").visitorType("Resident")
                .crossTowerPassValid(true).centralGateSync(true).syncDelayMs(180)
                .rfidRecognizedAcrossTowers(true).anprCameraMatched(true).vehicleNumber("KA-01-MH-9876").boomBarrierOpened(true)
                .testResult(FederatedGateTest.TestResult.FAIL)
                .failureReason("ANPR allowed Tower B resident into Tower D parking — cross-tower parking restriction not enforced").build();

        var g3 = FederatedGateTest.builder().communityId("MEGA-001").testCaseCode("GATE-003").testCaseName("Emergency vehicle — all gates open simultaneously")
                .scenario(FederatedGateTest.GateScenario.EMERGENCY_VEHICLE_ALL_GATES_OPEN)
                .sourceTower("Main Gate").destinationTower("ALL").visitorType("Ambulance")
                .crossTowerPassValid(true).centralGateSync(true).syncDelayMs(45)
                .rfidRecognizedAcrossTowers(false).anprCameraMatched(true).vehicleNumber("KA-01-AM-1234").boomBarrierOpened(true)
                .testResult(FederatedGateTest.TestResult.PASS).failureReason("").build();

        var a1 = SharedAmenityTest.builder().communityId("MEGA-001").testCaseCode("AMN-001").testCaseName("Cross-tower pool booking within quota")
                .scenario(SharedAmenityTest.AmenityScenario.CROSS_TOWER_POOL_BOOKING)
                .amenityName("Swimming Pool").bookingTower("Tower A").amenityTower("Tower C")
                .crossTowerBookingAllowed(true).quotaEnforced(true).towerQuota(10).currentBookings(7)
                .doubleBookingBlocked(false).billingToCorrectTower(true).amountCharged(500).billingPeriod("Monthly")
                .testResult(SharedAmenityTest.TestResult.PASS).failureReason("").build();

        var a2 = SharedAmenityTest.builder().communityId("MEGA-001").testCaseCode("AMN-002").testCaseName("Club house quota exceeded — booking not blocked")
                .scenario(SharedAmenityTest.AmenityScenario.CLUB_HOUSE_QUOTA_EXCEEDED)
                .amenityName("Club House").bookingTower("Tower B").amenityTower("Tower A")
                .crossTowerBookingAllowed(true).quotaEnforced(false).towerQuota(5).currentBookings(8)
                .doubleBookingBlocked(false).billingToCorrectTower(true).amountCharged(2000).billingPeriod("Per Event")
                .testResult(SharedAmenityTest.TestResult.FAIL)
                .failureReason("Tower B exceeded quota of 5 bookings (8 current) — quota enforcement bypassed for club house").build();

        var a3 = SharedAmenityTest.builder().communityId("MEGA-001").testCaseCode("AMN-003").testCaseName("Billing split correctly across 3 towers")
                .scenario(SharedAmenityTest.AmenityScenario.BILLING_SPLIT_ACROSS_TOWERS)
                .amenityName("Common Parking").bookingTower("Federation").amenityTower("ALL")
                .crossTowerBookingAllowed(true).quotaEnforced(true).towerQuota(100).currentBookings(67)
                .doubleBookingBlocked(true).billingToCorrectTower(true).amountCharged(15000).billingPeriod("Quarterly")
                .testResult(SharedAmenityTest.TestResult.PASS).failureReason("").build();

        var b1 = ConsolidatedBillingTest.builder().communityId("MEGA-001").testCaseCode("BIL-001").testCaseName("Multi-tower consolidated invoice generation")
                .scenario(ConsolidatedBillingTest.BillingScenario.MULTI_TOWER_CONSOLIDATED_INVOICE)
                .towerName("Tower A + Tower B").invoiceType("Maintenance")
                .towerAmount(45000).federationLevy(5000).totalAmount(50000)
                .gstApplied(true).gstAmount(9000).invoiceMerged(true).paymentSplitCorrect(true)
                .paymentGateway("Razorpay").receiptGenerated(true)
                .testResult(ConsolidatedBillingTest.TestResult.PASS).failureReason("").build();

        var b2 = ConsolidatedBillingTest.builder().communityId("MEGA-001").testCaseCode("BIL-002").testCaseName("GST split across separate legal entities")
                .scenario(ConsolidatedBillingTest.BillingScenario.GST_SPLIT_ACROSS_ENTITIES)
                .towerName("Tower C (Entity 1) + Tower D (Entity 2)").invoiceType("Maintenance + Amenity")
                .towerAmount(60000).federationLevy(8000).totalAmount(68000)
                .gstApplied(true).gstAmount(12240).invoiceMerged(false).paymentSplitCorrect(false)
                .paymentGateway("Razorpay").receiptGenerated(true)
                .testResult(ConsolidatedBillingTest.TestResult.FAIL)
                .failureReason("GST credited to Entity 1 GSTIN for both towers — Entity 2 share ₹4,320 misallocated").build();

        var b3 = ConsolidatedBillingTest.builder().communityId("MEGA-001").testCaseCode("BIL-003").testCaseName("Federation levy calculation — pro-rata by flat count")
                .scenario(ConsolidatedBillingTest.BillingScenario.FEDERATION_LEVY_CALCULATION)
                .towerName("ALL (4 towers, 480 flats)").invoiceType("Federation Levy")
                .towerAmount(0).federationLevy(240000).totalAmount(240000)
                .gstApplied(false).gstAmount(0).invoiceMerged(true).paymentSplitCorrect(true)
                .paymentGateway("Razorpay").receiptGenerated(true)
                .testResult(ConsolidatedBillingTest.TestResult.PASS).failureReason("").build();

        gateRepo.saveAll(List.of(g1, g2, g3));
        amenityRepo.saveAll(List.of(a1, a2, a3));
        billingRepo.saveAll(List.of(b1, b2, b3));

        return ResponseEntity.ok(new MegaTownshipDashboard(9, 6, 3, 0, 66.7,
                List.of(g1, g2, g3), List.of(a1, a2, a3), List.of(b1, b2, b3)));
    }
}

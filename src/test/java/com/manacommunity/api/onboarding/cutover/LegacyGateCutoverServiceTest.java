package com.manacommunity.api.onboarding.cutover;

import com.manacommunity.api.onboarding.cutover.model.*;
import com.manacommunity.api.onboarding.cutover.service.LegacyGateCutoverService;
import com.manacommunity.api.onboarding.cutover.service.LegacyGateCutoverServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LegacyGateCutoverServiceTest {

    private LegacyGateCutoverServiceImpl service;
    private final Long communityId = 5001L;

    @BeforeEach
    void setUp() {
        service = new LegacyGateCutoverServiceImpl();
    }

    @Test
    @DisplayName("Should generate tablet prewarm bundle with SHA-256 and compressed size")
    void testTabletPrewarmBundleGeneration() {
        List<TabletPrewarmBundleDto.SchoolBusPass> buses = Arrays.asList(
                TabletPrewarmBundleDto.SchoolBusPass.builder()
                        .registrationNumber("KA-01-F-9999")
                        .schoolName("Delhi Public School")
                        .driverName("Gopal Rao")
                        .driverPhone("+919876500001")
                        .studentFlatNumbers(Arrays.asList("A-101", "B-202", "C-303"))
                        .active(true)
                        .build()
        );

        List<LegacyGateCutoverService.DomesticStaffFastPassDto> staff = Arrays.asList(
                LegacyGateCutoverService.DomesticStaffFastPassDto.builder()
                        .staffId("STAFF-01")
                        .staffName("Sunita Devi")
                        .category("COOK")
                        .rfidCardNumber("CARD-12345678")
                        .authorizedFlats(Arrays.asList("A-101", "A-102"))
                        .active(true)
                        .build()
        );

        List<TabletPrewarmBundleDto.VehicleRfidTag> rfids = Arrays.asList(
                TabletPrewarmBundleDto.VehicleRfidTag.builder()
                        .epcHex("E280110520000001")
                        .plateNumber("KA-05-AB-1212")
                        .towerFlat("TOWER_A-101")
                        .active(true)
                        .build()
        );

        TabletPrewarmBundleDto bundle = service.generateTabletPrewarmBundle(communityId, buses, staff, rfids, 2000);

        assertNotNull(bundle);
        assertEquals(1, bundle.getSchoolBusesCount());
        assertEquals(1, bundle.getDomesticStaffCount());
        assertEquals(1, bundle.getVehicleRfidsCount());
        assertEquals(2000, bundle.getResidentWhitelistCount());
        assertNotNull(bundle.getBundleChecksumSha256());
        assertTrue(bundle.getCompressedBundleSizeBytes() > 65536);
        assertTrue(bundle.getDownloadUrl().contains(".sqlite.gz"));
    }

    @Test
    @DisplayName("Should abort cutover if any guard tablet has not pre-warmed offline SQLite cache")
    void testCutoverAbortOnMissingTabletPrewarm() {
        // Register 2 gate tablets
        service.registerTabletDevice(communityId, "TABLET-MAIN-IN-01", "GATE-1");
        service.registerTabletDevice(communityId, "TABLET-REAR-IN-02", "GATE-2");

        TabletPrewarmBundleDto bundle = service.generateTabletPrewarmBundle(
                communityId, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), 100);

        // Tablet 1 acks prewarm, but Tablet 2 fails/misses
        service.recordTabletPrewarmAck(communityId, "TABLET-MAIN-IN-01", bundle.getBundleChecksumSha256());

        CutoverReadinessAuditDto audit = service.auditCutoverReadiness(communityId, LocalDateTime.now());

        assertNotNull(audit);
        assertFalse(audit.isReadyFor6AmCutover());
        assertEquals(1, audit.getTabletsPendingPrewarm().size());
        assertTrue(audit.getTabletsPendingPrewarm().contains("TABLET-REAR-IN-02"));

        // Attempting cutover must abort
        CutoverExecutionResultDto execution = service.executeCutover(communityId);
        assertEquals(CutoverExecutionResultDto.Status.CUTOVER_ABORTED_PREWARM_MISSING, execution.getStatus());
        assertFalse(execution.isPeakMorningTrafficReady());
    }

    @Test
    @DisplayName("Should execute seamless 06:00 AM cutover when all tablets prewarmed and shadow dual-write synced")
    void testSuccessfulCutoverExecution() {
        service.registerTabletDevice(communityId, "TABLET-MAIN-IN", "GATE-1");
        service.registerTabletDevice(communityId, "TABLET-MAIN-OUT", "GATE-1");

        List<TabletPrewarmBundleDto.VehicleRfidTag> rfids = Arrays.asList(
                TabletPrewarmBundleDto.VehicleRfidTag.builder().epcHex("EPC-01").plateNumber("KA01A1").active(true).build()
        );

        TabletPrewarmBundleDto bundle = service.generateTabletPrewarmBundle(
                communityId, Collections.emptyList(), Collections.emptyList(), rfids, 500);

        // Both tablets acknowledge checksum
        service.recordTabletPrewarmAck(communityId, "TABLET-MAIN-IN", bundle.getBundleChecksumSha256());
        service.recordTabletPrewarmAck(communityId, "TABLET-MAIN-OUT", bundle.getBundleChecksumSha256());

        // Ingest shadow dual-write event from legacy proxy
        ShadowDualWritePassageDto shadowEvent = ShadowDualWritePassageDto.builder()
                .passageId("SHADOW-01")
                .communityId(communityId)
                .gateIdentifier("GATE-1")
                .rfidTagOrPlate("KA01A1")
                .passageType("RESIDENT_VEHICLE")
                .legacyTimestamp(LocalDateTime.now().minusSeconds(2))
                .build();
        service.ingestShadowDualWriteEvent(shadowEvent);

        CutoverReadinessAuditDto audit = service.auditCutoverReadiness(communityId, LocalDateTime.now());
        assertTrue(audit.isReadyFor6AmCutover());
        assertEquals(2, audit.getTabletsPrewarmedAndReady());
        assertEquals(0, audit.getTabletsPendingPrewarm().size());

        CutoverExecutionResultDto result = service.executeCutover(communityId);

        assertNotNull(result);
        assertEquals(CutoverExecutionResultDto.Status.CUTOVER_SUCCESSFUL, result.getStatus());
        assertTrue(result.isPeakMorningTrafficReady());
        assertEquals(2, result.getActiveGateControllersCount());
        assertTrue(result.getAuditLog().stream().anyMatch(l -> l.contains("CUTOVER COMPLETED")));
    }
}

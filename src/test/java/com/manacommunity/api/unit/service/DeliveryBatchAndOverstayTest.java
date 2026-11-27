package com.manacommunity.api.unit.service;

import com.manacommunity.api.delivery.dto.*;
import com.manacommunity.api.delivery.model.*;
import com.manacommunity.api.delivery.repository.ExpressDeliveryManifestRepository;
import com.manacommunity.api.delivery.repository.ExpressDeliveryPassRepository;
import com.manacommunity.api.delivery.repository.ResidentDeliveryPreferenceRepository;
import com.manacommunity.api.delivery.service.OverstaySlaEngineService;
import com.manacommunity.api.delivery.service.impl.ExpressDeliveryPartnerServiceImpl;
import com.manacommunity.api.visitor.entity.VisitorPass;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryBatchAndOverstayTest {

    @Mock
    private ExpressDeliveryPassRepository passRepository;

    @Mock
    private ResidentDeliveryPreferenceRepository preferenceRepository;

    @Mock
    private ExpressDeliveryManifestRepository manifestRepository;

    private ExpressDeliveryPartnerServiceImpl service;
    private OverstaySlaEngineService overstaySlaEngineService;

    @BeforeEach
    void setUp() {
        service = new ExpressDeliveryPartnerServiceImpl(passRepository, preferenceRepository);
        service.setManifestRepository(manifestRepository);
        overstaySlaEngineService = new OverstaySlaEngineService();
    }

    @Test
    @DisplayName("Should coalesce 18 courier packages across 15 flats into one EXP-B manifest")
    void shouldRegisterBatchDispatchWithFlatCoalescing() {
        List<ExpressBatchDispatchRequest.BatchPackageItem> items = new ArrayList<>();
        // 15 unique flats, with flats 1..3 receiving 2 packages each (total 18 packages)
        for (int i = 1; i <= 15; i++) {
            items.add(ExpressBatchDispatchRequest.BatchPackageItem.builder()
                    .orderId("AMZ-" + i)
                    .flatNumber("T1-" + (100 + i))
                    .residentName("Resident " + i)
                    .build());
        }
        for (int i = 1; i <= 3; i++) {
            items.add(ExpressBatchDispatchRequest.BatchPackageItem.builder()
                    .orderId("AMZ-EXTRA-" + i)
                    .flatNumber("T1-" + (100 + i))
                    .residentName("Resident " + i)
                    .build());
        }

        ExpressBatchDispatchRequest req = ExpressBatchDispatchRequest.builder()
                .partner(DeliveryPartner.AMAZON)
                .communityId(1L)
                .riderName("Suresh Courier")
                .riderPhone("+919800001122")
                .vehicleNumber("KA-01-XY-9988")
                .packages(items)
                .build();

        when(preferenceRepository.findByCommunityIdAndFlatNumber(anyLong(), anyString())).thenReturn(Optional.empty());
        when(manifestRepository.save(any(ExpressDeliveryManifest.class))).thenAnswer(inv -> {
            ExpressDeliveryManifest m = inv.getArgument(0);
            m.setId(500L);
            return m;
        });
        when(passRepository.save(any(ExpressDeliveryPass.class))).thenAnswer(inv -> inv.getArgument(0));

        ExpressBatchManifestResponse res = service.registerBatchDispatch(req, "AMAZON_KEY");

        assertThat(res.getSuccess()).isTrue();
        assertThat(res.getManifestCode()).startsWith("EXP-B-");
        assertThat(res.getTotalPackages()).isEqualTo(18);
        assertThat(res.getDistinctFlats()).isEqualTo(15);
        assertThat(res.getDistinctFlatsNotified()).isEqualTo(15);
        assertThat(res.getPendingPackages()).isEqualTo(18);
        assertThat(res.getPackages()).hasSize(18);
        verify(preferenceRepository, times(15)).findByCommunityIdAndFlatNumber(anyLong(), anyString());
    }

    @Test
    @DisplayName("Should clear all child passes in 1 tap and reconcile partial RTO on exit")
    void shouldVerifyBatchManifestAndReconcileExitRto() {
        LocalDateTime now = LocalDateTime.now();
        ExpressDeliveryManifest manifest = ExpressDeliveryManifest.builder()
                .id(500L)
                .communityId(1L)
                .partner(DeliveryPartner.AMAZON)
                .manifestCode("EXP-B-4410")
                .riderName("Suresh Courier")
                .riderPhone("+919800001122")
                .totalPackages(3)
                .distinctFlats(2)
                .status(ExpressPassStatus.PRE_APPROVED)
                .validFrom(now.minusMinutes(5))
                .validUntil(now.plusMinutes(120))
                .build();

        List<ExpressDeliveryPass> children = List.of(
                ExpressDeliveryPass.builder().id(1L).manifestId(500L).orderId("ORD-1").flatNumber("A-101").status(ExpressPassStatus.PRE_APPROVED).build(),
                ExpressDeliveryPass.builder().id(2L).manifestId(500L).orderId("ORD-2").flatNumber("A-101").status(ExpressPassStatus.PRE_APPROVED).build(),
                ExpressDeliveryPass.builder().id(3L).manifestId(500L).orderId("ORD-3").flatNumber("A-102").status(ExpressPassStatus.PRE_APPROVED).build()
        );

        when(manifestRepository.findByManifestCode("EXP-B-4410")).thenReturn(Optional.of(manifest));
        when(manifestRepository.save(any(ExpressDeliveryManifest.class))).thenAnswer(inv -> inv.getArgument(0));
        when(passRepository.findByManifestIdOrderByIdAsc(500L)).thenReturn(children);
        when(passRepository.save(any(ExpressDeliveryPass.class))).thenAnswer(inv -> inv.getArgument(0));

        ExpressBatchManifestResponse verifyRes = service.verifyBatchManifest(
                ExpressVerifyRequest.builder().entryCode("EXP-B-4410").gateId("GATE_1").build()
        );

        assertThat(verifyRes.getAllowed()).isTrue();
        assertThat(verifyRes.getBarrierAction()).isEqualTo("OPEN_BOOM_BARRIER");
        assertThat(verifyRes.getStatus()).isEqualTo(ExpressPassStatus.CHECKED_IN);
        assertThat(children).allMatch(c -> c.getStatus() == ExpressPassStatus.CHECKED_IN);

        ExpressBatchManifestResponse exitRes = service.reconcileBatchExit(
                ExpressBatchCheckoutRequest.builder()
                        .manifestCode("EXP-B-4410")
                        .gateId("GATE_OUT")
                        .undeliveredOrderIds(List.of("ORD-3"))
                        .build()
        );

        assertThat(exitRes.getStatus()).isEqualTo(ExpressPassStatus.RTO_CARRIED_OUT);
        assertThat(exitRes.getDeliveredPackages()).isEqualTo(2);
        assertThat(exitRes.getPendingPackages()).isEqualTo(1);
        assertThat(children.get(0).getDeliveryOutcome()).isEqualTo("DELIVERED");
        assertThat(children.get(2).getStatus()).isEqualTo(ExpressPassStatus.RTO_CARRIED_OUT);
        assertThat(children.get(2).getDeliveryOutcome()).isEqualTo("RTO_CARRIED_OUT");
    }

    @Test
    @DisplayName("Should mark single dispatch as AWAITING_APPROVAL when resident opted out")
    void shouldMarkAwaitingApprovalWhenAutoApproveDisabled() {
        when(preferenceRepository.findByCommunityIdAndFlatNumber(1L, "B-204"))
                .thenReturn(Optional.of(ResidentDeliveryPreference.builder()
                        .communityId(1L)
                        .flatNumber("B-204")
                        .autoApproveBlinkit(false)
                        .build()));
        when(passRepository.findByPartnerAndOrderId(DeliveryPartner.BLINKIT, "BLNK-909")).thenReturn(Optional.empty());
        when(passRepository.save(any(ExpressDeliveryPass.class))).thenAnswer(inv -> inv.getArgument(0));

        ExpressEntryResponse res = service.registerDispatch(ExpressEntryDispatchRequest.builder()
                .partner(DeliveryPartner.BLINKIT)
                .orderId("BLNK-909")
                .communityId(1L)
                .flatNumber("B-204")
                .riderName("Ramesh")
                .build(), "BLINKIT_KEY");

        assertThat(res.getStatus()).isEqualTo(ExpressPassStatus.AWAITING_APPROVAL);
    }

    @Test
    @DisplayName("Should apply category-aware SLAs without false positives for service technicians")
    void shouldApplyCategoryAwareOverstaySlas() {
        LocalDateTime now = LocalDateTime.now();

        ExpressDeliveryPass quickComm = ExpressDeliveryPass.builder()
                .partner(DeliveryPartner.BLINKIT)
                .riderName("Quick Rider")
                .status(ExpressPassStatus.CHECKED_IN)
                .checkedInAt(now.minusMinutes(50))
                .build();
        var quickEval = overstaySlaEngineService.evaluateExpressPass(quickComm, now);
        assertThat(quickEval.overstayed()).isTrue();
        assertThat(quickEval.action()).isEqualTo(OverstaySlaEngineService.OverstayAction.SECURITY_SUPERVISOR_ALERT);

        ExpressDeliveryManifest bulk15Flats = ExpressDeliveryManifest.builder()
                .partner(DeliveryPartner.AMAZON)
                .manifestCode("EXP-B-15")
                .distinctFlats(15)
                .status(ExpressPassStatus.CHECKED_IN)
                .checkedInAt(now.minusMinutes(50))
                .build();
        var bulkWithinSla = overstaySlaEngineService.evaluateManifest(bulk15Flats, now);
        assertThat(bulkWithinSla.overstayed()).isFalse();
        assertThat(bulkWithinSla.slaLimitMinutes()).isEqualTo(60);

        VisitorPass serviceTech = VisitorPass.builder()
                .visitorName("Urban Company Plumber")
                .passType(VisitorPass.PassType.SERVICE)
                .status(VisitorPass.PassStatus.CHECKED_IN)
                .checkedInAt(now.minusMinutes(195))
                .build();
        var serviceEval = overstaySlaEngineService.evaluateVisitorPass(serviceTech, now);
        assertThat(serviceEval.overstayed()).isTrue();
        assertThat(serviceEval.action()).isEqualTo(OverstaySlaEngineService.OverstayAction.RESIDENT_CONFIRMATION_PING);
    }
}

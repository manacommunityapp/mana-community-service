package com.manacommunity.api.unit.service;

import com.manacommunity.api.delivery.dto.*;
import com.manacommunity.api.delivery.model.*;
import com.manacommunity.api.delivery.repository.ExpressDeliveryPassRepository;
import com.manacommunity.api.delivery.repository.ResidentDeliveryPreferenceRepository;
import com.manacommunity.api.delivery.service.impl.ExpressDeliveryPartnerServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExpressDeliveryPartnerServiceTest {

    @Mock
    private ExpressDeliveryPassRepository passRepository;

    @Mock
    private ResidentDeliveryPreferenceRepository preferenceRepository;

    @InjectMocks
    private ExpressDeliveryPartnerServiceImpl service;

    private ExpressEntryDispatchRequest dispatchReq;

    @BeforeEach
    void setUp() {
        dispatchReq = ExpressEntryDispatchRequest.builder()
                .partner(DeliveryPartner.BLINKIT)
                .orderId("BLNK-ORD-101")
                .communityId(1L)
                .flatNumber("Tower A - 302")
                .residentName("Anita Rao")
                .riderName("Kishan Patel")
                .riderPhone("+919876543210")
                .vehicleNumber("KA-05-JK-1234")
                .etaMinutes(10)
                .itemsSummary("Groceries & Snacks")
                .build();
    }

    @Test
    @DisplayName("Should successfully issue Express Entry pass on partner dispatch")
    void shouldRegisterDispatchSuccessfully() {
        when(preferenceRepository.findByCommunityIdAndFlatNumber(1L, "Tower A - 302"))
                .thenReturn(Optional.of(ResidentDeliveryPreference.builder()
                        .communityId(1L)
                        .flatNumber("Tower A - 302")
                        .autoApproveBlinkit(true)
                        .defaultLeaveAtGate(false)
                        .deliveryInstructions("Leave at door")
                        .build()));

        when(passRepository.findByPartnerAndOrderId(DeliveryPartner.BLINKIT, "BLNK-ORD-101"))
                .thenReturn(Optional.empty());

        when(passRepository.save(any(ExpressDeliveryPass.class))).thenAnswer(inv -> {
            ExpressDeliveryPass p = inv.getArgument(0);
            p.setId(99L);
            return p;
        });

        ExpressEntryResponse res = service.registerDispatch(dispatchReq, "BLINKIT_API_KEY");

        assertThat(res.getSuccess()).isTrue();
        assertThat(res.getPartner()).isEqualTo(DeliveryPartner.BLINKIT);
        assertThat(res.getEntryPassCode()).startsWith("EXP-");
        assertThat(res.getStatus()).isEqualTo(ExpressPassStatus.PRE_APPROVED);
        assertThat(res.getGateInstructions()).isEqualTo("Leave at door");
        verify(passRepository).save(any(ExpressDeliveryPass.class));
    }

    @Test
    @DisplayName("Should fast-verify and clear boom barrier for valid express code")
    void shouldVerifyEntryFastAndClearBoomBarrier() {
        ExpressDeliveryPass pass = ExpressDeliveryPass.builder()
                .id(99L)
                .entryPassCode("EXP-4521")
                .barcodePayload("EXP:1:BLINKIT:BLNK-ORD-101:EXP-4521")
                .partner(DeliveryPartner.BLINKIT)
                .orderId("BLNK-ORD-101")
                .flatNumber("Tower A - 302")
                .riderName("Kishan Patel")
                .status(ExpressPassStatus.PRE_APPROVED)
                .validFrom(LocalDateTime.now().minusMinutes(5))
                .validUntil(LocalDateTime.now().plusMinutes(60))
                .build();

        when(passRepository.findByEntryPassCode("EXP-4521")).thenReturn(Optional.of(pass));
        when(passRepository.save(any(ExpressDeliveryPass.class))).thenReturn(pass);

        ExpressVerifyRequest verifyReq = ExpressVerifyRequest.builder()
                .entryCode("EXP-4521")
                .gateId("GATE_NORTH")
                .build();

        ExpressVerifyResponse verifyRes = service.verifyEntry(verifyReq);

        assertThat(verifyRes.getAllowed()).isTrue();
        assertThat(verifyRes.getBarrierAction()).isEqualTo("OPEN_BOOM_BARRIER");
        assertThat(verifyRes.getStatus()).isEqualTo(ExpressPassStatus.CHECKED_IN);
        assertThat(verifyRes.getMessage()).contains("PRE-APPROVED");
    }

    @Test
    @DisplayName("Should reject verification if pass is expired")
    void shouldRejectExpiredPass() {
        ExpressDeliveryPass pass = ExpressDeliveryPass.builder()
                .id(99L)
                .entryPassCode("EXP-4521")
                .partner(DeliveryPartner.BLINKIT)
                .validFrom(LocalDateTime.now().minusHours(3))
                .validUntil(LocalDateTime.now().minusMinutes(10))
                .status(ExpressPassStatus.PRE_APPROVED)
                .build();

        when(passRepository.findByEntryPassCode("EXP-4521")).thenReturn(Optional.of(pass));

        ExpressVerifyRequest verifyReq = ExpressVerifyRequest.builder()
                .entryCode("EXP-4521")
                .build();

        ExpressVerifyResponse verifyRes = service.verifyEntry(verifyReq);

        assertThat(verifyRes.getAllowed()).isFalse();
        assertThat(verifyRes.getBarrierAction()).isEqualTo("HOLD");
        assertThat(verifyRes.getMessage()).contains("expired");
    }

    @Test
    @DisplayName("Should record checkout exit and complete delivery cycle")
    void shouldCheckoutExitSuccessfully() {
        ExpressDeliveryPass pass = ExpressDeliveryPass.builder()
                .id(99L)
                .entryPassCode("EXP-4521")
                .partner(DeliveryPartner.BLINKIT)
                .status(ExpressPassStatus.CHECKED_IN)
                .build();

        when(passRepository.findByEntryPassCode("EXP-4521")).thenReturn(Optional.of(pass));
        when(passRepository.save(any(ExpressDeliveryPass.class))).thenReturn(pass);

        ExpressCheckoutRequest checkoutReq = ExpressCheckoutRequest.builder()
                .identifier("EXP-4521")
                .gateId("GATE_OUT")
                .build();

        ExpressVerifyResponse checkoutRes = service.checkoutExit(checkoutReq);

        assertThat(checkoutRes.getAllowed()).isTrue();
        assertThat(checkoutRes.getStatus()).isEqualTo(ExpressPassStatus.COMPLETED);
    }
}

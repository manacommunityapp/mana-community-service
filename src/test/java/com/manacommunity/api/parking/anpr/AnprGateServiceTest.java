package com.manacommunity.api.parking.anpr;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.parking.anpr.dto.AnprWebhookPayload;
import com.manacommunity.api.parking.anpr.dto.AnprWebhookResponse;
import com.manacommunity.api.parking.anpr.entity.AnprGateEvent;
import com.manacommunity.api.parking.anpr.entity.AnprGateEvent.BarrierAction;
import com.manacommunity.api.parking.anpr.repository.AnprGateEventRepository;
import com.manacommunity.api.parking.anpr.service.AnprGateService;
import com.manacommunity.api.parking.anpr.service.AnprPlateNormalizer;
import com.manacommunity.api.parking.entity.ParkingVisitorPass;
import com.manacommunity.api.parking.entity.ResidentVehicle;
import com.manacommunity.api.parking.repository.ParkingVisitorPassRepository;
import com.manacommunity.api.parking.repository.ResidentVehicleRepository;
import com.manacommunity.api.repository.CommunityRepository;
import com.manacommunity.api.user.model.AppUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ANPR Gate Service Unit Tests")
class AnprGateServiceTest {

    @Mock AnprGateEventRepository eventRepository;
    @Mock ResidentVehicleRepository vehicleRepository;
    @Mock ParkingVisitorPassRepository visitorPassRepository;
    @Mock CommunityRepository communityRepository;
    @Mock SimpMessagingTemplate messagingTemplate;
    @Spy  AnprPlateNormalizer normalizer;

    @InjectMocks AnprGateService anprGateService;

    private Community community;
    private AppUser resident;

    @BeforeEach
    void setUp() {
        community = new Community();
        community.setId(1L);
        community.setName("Test Society");

        resident = new AppUser();
        resident.setId(10L);
        resident.setFullName("Ramesh Kumar");

        when(communityRepository.findById(1L)).thenReturn(Optional.of(community));
        when(eventRepository.save(any(AnprGateEvent.class))).thenAnswer(inv -> {
            AnprGateEvent e = inv.getArgument(0);
            e.setId(99L);
            return e;
        });
    }

    private AnprWebhookPayload payload(String plate, double confidence) {
        return new AnprWebhookPayload(42L, "GATE_MAIN_IN", "ENTRY",
                plate, confidence, "SUCCESS",
                LocalDateTime.now().toString(), "mana-anpr-service");
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Resident vehicle matching")
    class ResidentVehicleTests {

        @Test
        @DisplayName("Should OPEN barrier when plate matches registered resident vehicle")
        void shouldOpenForResidentVehicle() {
            ResidentVehicle vehicle = ResidentVehicle.builder()
                    .id(5L).numberPlate("MH12AB1234").owner(resident).community(community).build();

            when(vehicleRepository.findByCommunityIdAndNumberPlate(1L, "MH12AB1234"))
                    .thenReturn(Optional.of(vehicle));

            AnprWebhookResponse response = anprGateService.processWebhook(1L, payload("MH12AB1234", 0.95));

            assertThat(response.barrierAction()).isEqualTo(BarrierAction.OPEN);
            assertThat(response.matchedResidentName()).isEqualTo("Ramesh Kumar");
            verify(eventRepository).save(argThat(e -> e.getBarrierAction() == BarrierAction.OPEN));
        }

        @Test
        @DisplayName("Should normalise plate before lookup (strips spaces and hyphens)")
        void shouldNormalisePlateBeforeLookup() {
            ResidentVehicle vehicle = ResidentVehicle.builder()
                    .id(5L).numberPlate("MH12AB1234").owner(resident).community(community).build();

            when(vehicleRepository.findByCommunityIdAndNumberPlate(1L, "MH12AB1234"))
                    .thenReturn(Optional.of(vehicle));

            // Plate arrives with spaces from OCR
            AnprWebhookResponse response = anprGateService.processWebhook(1L, payload("MH 12 AB 1234", 0.92));

            assertThat(response.barrierAction()).isEqualTo(BarrierAction.OPEN);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Visitor pass matching")
    class VisitorPassTests {

        @Test
        @DisplayName("Should OPEN barrier when plate matches active visitor pass within validity window")
        void shouldOpenForValidVisitorPass() {
            ParkingVisitorPass pass = ParkingVisitorPass.builder()
                    .id(20L)
                    .visitorName("Suresh Visitor")
                    .vehicleNumber("KA01MN5678")
                    .status("ACTIVE")
                    .validFrom(LocalDateTime.now().minusHours(1))
                    .validUntil(LocalDateTime.now().plusHours(3))
                    .community(community)
                    .build();

            when(vehicleRepository.findByCommunityIdAndNumberPlate(1L, "KA01MN5678"))
                    .thenReturn(Optional.empty());
            when(visitorPassRepository.findByCommunityIdAndStatus(1L, "ACTIVE"))
                    .thenReturn(List.of(pass));

            AnprWebhookResponse response = anprGateService.processWebhook(1L, payload("KA01MN5678", 0.90));

            assertThat(response.barrierAction()).isEqualTo(BarrierAction.OPEN);
            assertThat(response.matchedResidentName()).isEqualTo("Suresh Visitor");
        }

        @Test
        @DisplayName("Should HOLD when visitor pass is expired")
        void shouldHoldForExpiredVisitorPass() {
            ParkingVisitorPass expiredPass = ParkingVisitorPass.builder()
                    .id(21L)
                    .visitorName("Old Visitor")
                    .vehicleNumber("DL01CX9999")
                    .status("ACTIVE")
                    .validFrom(LocalDateTime.now().minusDays(2))
                    .validUntil(LocalDateTime.now().minusHours(1))  // expired
                    .community(community)
                    .build();

            when(vehicleRepository.findByCommunityIdAndNumberPlate(1L, "DL01CX9999"))
                    .thenReturn(Optional.empty());
            when(visitorPassRepository.findByCommunityIdAndStatus(1L, "ACTIVE"))
                    .thenReturn(List.of(expiredPass));

            AnprWebhookResponse response = anprGateService.processWebhook(1L, payload("DL01CX9999", 0.88));

            assertThat(response.barrierAction()).isEqualTo(BarrierAction.HOLD);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Unknown vehicle handling")
    class UnknownVehicleTests {

        @Test
        @DisplayName("Should HOLD and push WebSocket alert for completely unknown plate")
        void shouldHoldAndAlertForUnknownVehicle() {
            when(vehicleRepository.findByCommunityIdAndNumberPlate(1L, "TS09ZZ0000"))
                    .thenReturn(Optional.empty());
            when(visitorPassRepository.findByCommunityIdAndStatus(1L, "ACTIVE"))
                    .thenReturn(List.of());

            AnprWebhookResponse response = anprGateService.processWebhook(1L, payload("TS09ZZ0000", 0.85));

            assertThat(response.barrierAction()).isEqualTo(BarrierAction.HOLD);
            assertThat(response.matchedResidentName()).isNull();
            verify(messagingTemplate).convertAndSend(
                    contains("/anpr/alerts"), any(AnprGateService.AnprSecurityAlert.class));
        }

        @Test
        @DisplayName("Should still HOLD even with high ANPR confidence if plate unregistered")
        void shouldHoldEvenWithHighConfidenceUnknownPlate() {
            when(vehicleRepository.findByCommunityIdAndNumberPlate(anyLong(), anyString()))
                    .thenReturn(Optional.empty());
            when(visitorPassRepository.findByCommunityIdAndStatus(anyLong(), anyString()))
                    .thenReturn(List.of());

            AnprWebhookResponse response = anprGateService.processWebhook(1L, payload("MH99XY8888", 0.99));

            assertThat(response.barrierAction()).isEqualTo(BarrierAction.HOLD);
        }
    }
}

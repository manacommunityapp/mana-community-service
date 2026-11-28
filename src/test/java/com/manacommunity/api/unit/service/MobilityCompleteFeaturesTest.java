package com.manacommunity.api.unit.service;

import com.manacommunity.api.commerce.core.repository.CommerceOrderRepository;
import com.manacommunity.api.groupbuying.repository.GroupBuyOrderRepository;
import com.manacommunity.api.mobility.dto.*;
import com.manacommunity.api.mobility.model.*;
import com.manacommunity.api.mobility.repository.*;
import com.manacommunity.api.mobility.service.MobilityJourneyMatchingScheduler;
import com.manacommunity.api.mobility.service.MobilityPaymentCallbackService;
import com.manacommunity.api.mobility.service.impl.MobilityServiceImpl;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.payments.entity.PaymentReceipt;
import com.manacommunity.api.payments.repository.CashfreeOrderRepository;
import com.manacommunity.api.payments.service.CashfreePaymentService;
import com.manacommunity.api.payments.service.PaymentAmountResolver;
import com.manacommunity.api.payments.service.PaymentReceiptService;
import com.manacommunity.api.repository.CommunityRepository;
import com.manacommunity.api.service.ExpoPushService;
import com.manacommunity.api.storage.FileStorageService;
import com.manacommunity.api.trip.engine.CommuteMatchingEngine;
import com.manacommunity.api.user.model.AppUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MobilityCompleteFeaturesTest {

    @Mock MobilityTripRepository tripRepo;
    @Mock MobilityDriverRepository driverRepo;
    @Mock MobilityDriverDocumentRepository docRepo;
    @Mock MobilityVehicleRepository vehicleRepo;
    @Mock MobilityDriverOfferRepository offerRepo;
    @Mock MobilityCarpoolOfferRepository carpoolRepo;
    @Mock MobilityCarpoolBookingRepository carpoolBookingRepo;
    @Mock MobilityJourneySlotRepository journeySlotRepo;
    @Mock MobilityJourneyMatchRepository journeyMatchRepo;
    @Mock MobilityAirportRideRequestRepository airportRequestRepo;
    @Mock MobilityServiceZoneRepository serviceZoneRepo;
    @Mock CommunityRepository communityRepo;
    @Mock ExpoPushService expoPushService;
    @Mock FileStorageService fileStorageService;

    @Mock GroupBuyOrderRepository groupBuyOrderRepo;
    @Mock CommerceOrderRepository commerceOrderRepo;
    @Mock CashfreeOrderRepository cashfreeOrderRepo;
    @Mock PaymentReceiptService paymentReceiptService;
    @Mock ObjectProvider<CashfreePaymentService> cashfreeProvider;

    private CommuteMatchingEngine matchingEngine;
    private MobilityServiceImpl service;
    private MobilityPaymentCallbackService paymentCallbackService;
    private MobilityJourneyMatchingScheduler scheduler;

    private Community community;
    private AppUser resident;
    private AppUser otherResident;
    private MobilityServiceZone zone;

    @BeforeEach
    void setUp() {
        matchingEngine = new CommuteMatchingEngine();
        service = new MobilityServiceImpl(
                tripRepo, driverRepo, docRepo, vehicleRepo, offerRepo,
                carpoolRepo, carpoolBookingRepo, journeySlotRepo, journeyMatchRepo,
                airportRequestRepo, serviceZoneRepo, communityRepo,
                matchingEngine, expoPushService, fileStorageService
        );
        paymentCallbackService = new MobilityPaymentCallbackService(
                tripRepo, carpoolBookingRepo, paymentReceiptService, cashfreeProvider
        );
        scheduler = new MobilityJourneyMatchingScheduler(
                airportRequestRepo, carpoolRepo, journeyMatchRepo,
                journeySlotRepo, carpoolBookingRepo, matchingEngine, expoPushService
        );

        community = Community.builder().id(1L).name("Mana Heights").city("Hyderabad").type("APARTMENT").build();
        resident = AppUser.builder().id(101L).fullName("Asha Rao").phone("9876543210").community(community).build();
        otherResident = AppUser.builder().id(202L).fullName("Kiran Verma").phone("9876500000").community(community).build();
        zone = MobilityServiceZone.builder()
                .id(11L).community(community).name("Core Zone")
                .centerLat(17.4400).centerLng(78.3489)
                .radiusKm(new BigDecimal("10.00")).active(true).build();
    }

    @Test
    void requestRideRejectsOutOfZoneAndAcceptsInsideZone() {
        when(communityRepo.findById(1L)).thenReturn(Optional.of(community));
        when(serviceZoneRepo.findByCommunityIdAndActiveTrueOrderByNameAsc(1L)).thenReturn(List.of(zone));
        when(tripRepo.existsByPassengerIdAndIdempotencyKey(any(), any())).thenReturn(false);

        CreateMobilityTripRequest outside = new CreateMobilityTripRequest();
        outside.setServiceType(MobilityServiceType.CAB);
        outside.setPickupLat(12.9716); // Bengaluru (~500 km away)
        outside.setPickupLng(77.5946);
        outside.setPickupAddress("MG Road");
        outside.setDestLat(17.4500);
        outside.setDestLng(78.3600);
        outside.setDestAddress("Gate");
        outside.setIdempotencyKey("k-out");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.requestRide(resident, outside));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());

        CreateMobilityTripRequest inside = new CreateMobilityTripRequest();
        inside.setServiceType(MobilityServiceType.CAB);
        inside.setPickupLat(17.4410);
        inside.setPickupLng(78.3500);
        inside.setPickupAddress("Tower A");
        inside.setDestLat(17.4600);
        inside.setDestLng(78.3700);
        inside.setDestAddress("HITEC");
        inside.setPaymentMethod("UPI");
        inside.setIdempotencyKey("k-in");
        when(driverRepo.findOnlineApprovedDrivers()).thenReturn(List.of());
        when(tripRepo.save(any())).thenAnswer(inv -> {
            MobilityTrip t = inv.getArgument(0);
            t.setId(500L);
            return t;
        });

        MobilityTripDto result = service.requestRide(resident, inside);
        assertEquals(11L, result.getServiceZoneId());
        assertEquals("PENDING", result.getPaymentStatus());
        assertEquals("UPI", result.getPaymentMethod());
    }

    @Test
    void registerDriverAssignsValidatedZoneAndOpsCanCrudZones() {
        when(driverRepo.existsByUserId(101L)).thenReturn(false);
        when(serviceZoneRepo.findById(11L)).thenReturn(Optional.of(zone));
        when(driverRepo.save(any())).thenAnswer(inv -> {
            MobilityDriver d = inv.getArgument(0);
            d.setId(77L);
            return d;
        });

        RegisterDriverRequest req = new RegisterDriverRequest();
        req.setVehicleType(MobilityVehicleType.CAB);
        req.setVehicleModel("Dzire");
        req.setVehicleNumberPlate("TS09AB1234");
        req.setServiceZoneId(11L);

        MobilityDriverDto dto = service.registerDriver(resident, req);
        assertEquals(11L, dto.getServiceZoneId());
        assertEquals("Core Zone", dto.getServiceZoneName());

        when(communityRepo.findById(1L)).thenReturn(Optional.of(community));
        when(serviceZoneRepo.save(any())).thenAnswer(inv -> {
            MobilityServiceZone z = inv.getArgument(0);
            z.setId(12L);
            return z;
        });
        ServiceZoneRequest zr = new ServiceZoneRequest();
        zr.setName("Airport Corridor");
        zr.setCenterLat(17.2403);
        zr.setCenterLng(78.4294);
        zr.setRadiusKm(new BigDecimal("15.0"));
        MobilityServiceZoneDto created = service.createServiceZone(resident, zr);
        assertEquals(12L, created.getId());
        assertTrue(created.isActive());
    }

    @Test
    void paymentConfirmationReceiptRefundAndDriverSettlementWorkEndToEnd() {
        MobilityDriver driver = MobilityDriver.builder().id(77L).user(otherResident)
                .status(MobilityDriverStatus.APPROVED).availability("ONLINE").build();
        MobilityTrip trip = MobilityTrip.builder()
                .id(88L).tripRef("MB-88").community(community).passenger(resident).driver(driver)
                .serviceType(MobilityServiceType.CAB).status(MobilityTripStatus.COMPLETED)
                .pickupLat(17.44).pickupLng(78.35).pickupAddress("Home")
                .destLat(17.48).destLng(78.39).destAddress("Office")
                .quotedTotal(new BigDecimal("200.00")).finalFare(new BigDecimal("200.00"))
                .paymentMethod("UPI").paymentStatus("PENDING")
                .requestedAt(Instant.now().minusSeconds(900)).completedAt(Instant.now())
                .build();

        when(tripRepo.findById(88L)).thenReturn(Optional.of(trip));
        when(paymentReceiptService.issue(any())).thenAnswer(inv -> inv.getArgument(0));
        when(tripRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        paymentCallbackService.confirmTripPayment(88L, "MANA-88", "CF-PAY-1", "CASHFREE");
        assertEquals("PAID", trip.getPaymentStatus());
        assertEquals(new BigDecimal("20.00"), trip.getPlatformFee());
        assertEquals(new BigDecimal("180.00"), trip.getDriverPayout());
        assertEquals("MOB-RCP-MB-88", trip.getReceiptNumber());
        verify(paymentReceiptService).issue(any(PaymentReceipt.class));

        when(driverRepo.findByUserId(202L)).thenReturn(Optional.of(driver));
        when(tripRepo.findByDriverIdOrderByRequestedAtDesc(77L)).thenReturn(List.of(trip));
        DriverEarningsSummaryDto earnings = service.getMyEarnings(otherResident);
        assertEquals(new BigDecimal("180.00"), earnings.getPendingSettlement());
        assertEquals(BigDecimal.ZERO, earnings.getSettledEarnings());

        when(driverRepo.findById(77L)).thenReturn(Optional.of(driver));
        BigDecimal settled = service.settleDriverEarnings(resident, 77L);
        assertEquals(new BigDecimal("180.00"), settled);
        assertTrue(trip.isSettled());

        // Verify PaymentAmountResolver resolves server-derived MOBILITY_TRIP fare
        PaymentAmountResolver resolver = new PaymentAmountResolver(
                groupBuyOrderRepo, commerceOrderRepo, cashfreeOrderRepo, tripRepo, carpoolBookingRepo);
        trip.setPaymentStatus("PENDING");
        trip.setStatus(MobilityTripStatus.ASSIGNED);
        PaymentAmountResolver.ResolvedAmount resolved = resolver.resolve("MOBILITY_TRIP", 88L, 1L, 101L, new BigDecimal("1.00"));
        assertTrue(resolved.serverDerived());
        assertEquals(new BigDecimal("200.00"), resolved.amount());

        // Refund when cancelled after PAID
        trip.setPaymentStatus("PAID");
        trip.setPaymentOrderId("MANA-88");
        when(cashfreeProvider.getIfAvailable()).thenReturn(null);
        paymentCallbackService.refundTripOnCancellation(trip, "Plans changed");
        assertEquals("REFUNDED", trip.getRefundStatus());
        assertEquals(new BigDecimal("200.00"), trip.getRefundAmount());
    }

    @Test
    void schoolSafeguardingDetourMatchingAndRecurringSlotGeneration() {
        CreateJourneyRequest schoolReq = new CreateJourneyRequest();
        schoolReq.setJourneyType("SCHOOL");
        schoolReq.setDirection("TO_HUB");
        schoolReq.setHubCode("DPS");
        schoolReq.setArea("Gachibowli");
        schoolReq.setTravelAt(Instant.now().plusSeconds(3600).toString());
        // Missing guardian info must be rejected for SCHOOL safeguarding
        assertThrows(ResponseStatusException.class, () -> service.createJourneyRequest(resident, schoolReq));

        // Recurring slot creates offer on matching day without duplicates
        Instant now = Instant.now();
        String todayShort = now.atZone(ZoneId.of("Asia/Kolkata")).toLocalDate().plusDays(1).getDayOfWeek().name().substring(0, 3);
        MobilityJourneySlot slot = MobilityJourneySlot.builder()
                .id(31L).community(community).createdBy(otherResident)
                .journeyType("OFFICE").direction("TO_HUB").hubCode("HITEC")
                .origin("Mana Heights").destination("HITEC City")
                .departureTime(LocalTime.of(9, 0)).daysOfWeek(todayShort)
                .seatCapacity((short) 3).luggageCapacity((short) 1)
                .pricePerSeat(new BigDecimal("60.00")).autoGenerate(true).status("ACTIVE")
                .originLat(17.44).originLng(78.35).destLat(17.45).destLng(78.38)
                .build();
        when(journeySlotRepo.findByStatusAndAutoGenerateTrue("ACTIVE")).thenReturn(List.of(slot));
        when(carpoolRepo.existsBySourceSlotIdAndScheduledDate(eq(31L), any(LocalDate.class))).thenReturn(false);

        int generated = scheduler.generateRecurringSlotOffers(now);
        assertEquals(1, generated);
        verify(carpoolRepo).save(argThat(o -> Long.valueOf(31L).equals(o.getSourceSlotId()) && o.getAvailableSeats() == 3));

        // Geo detour filtering excludes offers beyond maxDetourKm
        MobilityCarpoolOffer nearOffer = MobilityCarpoolOffer.builder()
                .id(1L).community(community).driverUser(otherResident)
                .origin("Near").destination("HITEC").journeyType("OFFICE").hubCode("HITEC")
                .originLat(17.441).originLng(78.351).destLat(17.451).destLng(78.381)
                .maxDetourKm(5.0).allowCrossCommunity(true)
                .departureTime(now.plusSeconds(3600)).totalSeats((short) 3).availableSeats((short) 3).status("ACTIVE").build();
        MobilityCarpoolOffer farOffer = MobilityCarpoolOffer.builder()
                .id(2L).community(community).driverUser(otherResident)
                .origin("Far").destination("Airport").journeyType("OFFICE").hubCode("HITEC")
                .originLat(17.240).originLng(78.429).destLat(17.451).destLng(78.381)
                .maxDetourKm(5.0).allowCrossCommunity(true)
                .departureTime(now.plusSeconds(3600)).totalSeats((short) 3).availableSeats((short) 3).status("ACTIVE").build();

        when(carpoolRepo.findJourneyOffersWithScope(eq(1L), eq(true), eq("OFFICE"), eq("HITEC"),
                isNull(), isNull(), isNull(), isNull(), eq(1), eq(0), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(farOffer, nearOffer)));

        Page<MobilityCarpoolOfferDto> matched = service.searchJourneyOffers(
                resident, "OFFICE", "HITEC", null, null, null, 30, 1, 0,
                17.440, 78.350, 17.450, 78.380, 5.0, true, 0, 20);
        assertEquals(1, matched.getTotalElements());
        assertEquals(1L, matched.getContent().get(0).getId());
        assertNotNull(matched.getContent().get(0).getRouteDetourKm());
    }
}

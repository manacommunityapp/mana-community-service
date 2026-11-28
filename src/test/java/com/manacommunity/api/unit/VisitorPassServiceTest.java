package com.manacommunity.api.unit;

import com.manacommunity.api.delivery.model.ResidentDeliveryPreference;
import com.manacommunity.api.delivery.repository.ResidentDeliveryPreferenceRepository;
import com.manacommunity.api.delivery.service.OverstaySlaEngineService;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.parcel.entity.ParcelRecord;
import com.manacommunity.api.parcel.service.ParcelService;
import com.manacommunity.api.service.NotificationManagementService;
import com.manacommunity.api.platform.outbox.DomainEventPublisher;
import com.manacommunity.api.privacy.PiiMaskingService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import com.manacommunity.api.visitor.dto.VisitorPassRequest;
import com.manacommunity.api.visitor.dto.VisitorPassResponse;
import com.manacommunity.api.visitor.entity.GroupGuest;
import com.manacommunity.api.visitor.entity.GroupInvite;
import com.manacommunity.api.visitor.entity.VisitorPass;
import com.manacommunity.api.visitor.entity.VisitorPass.PassStatus;
import com.manacommunity.api.visitor.entity.VisitorPass.PassType;
import com.manacommunity.api.visitor.repository.GroupInviteRepository;
import com.manacommunity.api.visitor.repository.VisitorAuditLogRepository;
import com.manacommunity.api.visitor.repository.VisitorPassRepository;
import com.manacommunity.api.visitor.service.VisitorPassService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("VisitorPassService Unit Tests")
class VisitorPassServiceTest {

    @Mock VisitorPassRepository repo;
    @Mock VisitorAuditLogRepository auditLogRepo;
    @Mock AppUserRepository userRepo;
    @Mock NotificationManagementService notificationService;
    @Mock DomainEventPublisher domainEventPublisher;
    @Mock PiiMaskingService piiMaskingService;
    @Mock GroupInviteRepository groupInviteRepo;

    @InjectMocks VisitorPassService service;

    private Community community;
    private AppUser resident;
    private AppUser guard;

    private String hash(String otp) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(otp.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            return String.valueOf(otp.hashCode());
        }
    }

    @BeforeEach
    void setUp() {
        community = new Community();
        community.setId(1L);
        community.setName("Green Meadows");

        resident = new AppUser();
        resident.setId(10L);
        resident.setFullName("Arun Sharma");
        resident.setFlatNo("A-402");
        resident.setPhone("9876543210");
        resident.setCommunity(community);

        guard = new AppUser();
        guard.setId(99L);
        guard.setFullName("Guard Bahadur");
        guard.setCommunity(community);

        when(piiMaskingService.maskPhone(anyString())).thenAnswer(inv -> {
            String p = inv.getArgument(0);
            if (p == null || p.length() < 4) return p;
            return p.substring(0, 2) + "****" + p.substring(p.length() - 2);
        });

        when(repo.save(any(VisitorPass.class))).thenAnswer(inv -> {
            VisitorPass p = inv.getArgument(0);
            if (p.getId() == null) p.setId(100L);
            return p;
        });
    }

    @Test
    @DisplayName("createWalkIn resolves resident by flat number when residentId is not provided")
    void createWalkIn_autoResolvesResidentByFlat() {
        when(userRepo.findByCommunityIdAndFlatNoQuery(1L, "A-402")).thenReturn(List.of(resident));

        VisitorPassRequest req = new VisitorPassRequest();
        req.setVisitorName("Vikram Singh");
        req.setVisitorPhone("9811223344");
        req.setFlatNumber("A-402");
        req.setPurpose("Personal Visit");
        req.setPassType("GUEST");

        VisitorPassResponse resp = service.createWalkIn(req, guard, community);

        assertThat(resp).isNotNull();
        assertThat(resp.getResidentId()).isEqualTo(10L);
        assertThat(resp.getResidentName()).isEqualTo("Arun Sharma");
        assertThat(resp.getStatus()).isEqualTo(PassStatus.PENDING.name());
        verify(notificationService).createNotification(eq(10L), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("getApprovalStatus returns correct approval status map for polling")
    void getApprovalStatus_returnsStatusMap() {
        VisitorPass pass = VisitorPass.builder()
                .id(101L)
                .status(PassStatus.APPROVED)
                .build();
        when(repo.findById(101L)).thenReturn(Optional.of(pass));

        Map<String, Object> statusMap = service.getApprovalStatus(101L);

        assertThat(statusMap.get("approvalStatus")).isEqualTo("APPROVED");
        assertThat(statusMap.get("status")).isEqualTo("APPROVED");
    }

    @Test
    @DisplayName("generateDeliveryOtp creates delivery pass and returns masked resident phone")
    void generateDeliveryOtp_success() {
        when(userRepo.findByCommunityIdAndFlatNoQuery(1L, "A-402")).thenReturn(List.of(resident));

        Map<String, String> result = service.generateDeliveryOtp("A-402", "Amazon", guard, community);

        assertThat(result).isNotNull();
        assertThat(result.get("maskedPhone")).isEqualTo("98****10");
        assertThat(result.get("passCode")).isNotNull();
        verify(repo).save(argThat(p -> p.getPassType() == PassType.DELIVERY && p.getOtpHash() != null));
        verify(notificationService).createNotification(eq(10L), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("verifyDeliveryOtp transitions pass to CHECKED_IN and notifies resident")
    void verifyDeliveryOtp_success() {
        when(userRepo.findByCommunityIdAndFlatNoQuery(1L, "A-402")).thenReturn(List.of(resident));

        String otp = "654321";
        String hashed = hash(otp);

        VisitorPass deliveryPass = VisitorPass.builder()
                .id(105L)
                .passType(PassType.DELIVERY)
                .status(PassStatus.PENDING)
                .flatNumber("A-402")
                .otpHash(hashed)
                .otpExpiresAt(LocalDateTime.now().plusMinutes(10))
                .resident(resident)
                .community(community)
                .build();

        when(repo.findByOtpHash(hashed)).thenReturn(Optional.of(deliveryPass));

        VisitorPassResponse resp = service.verifyDeliveryOtp("A-402", otp, guard, community);

        assertThat(resp).isNotNull();
        assertThat(resp.getStatus()).isEqualTo("CHECKED_IN");
        verify(auditLogRepo).save(any());
        verify(notificationService).createNotification(eq(10L), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("leaveAtGate updates status to LEAVE_AT_GATE and records audit log")
    void leaveAtGate_success() {
        VisitorPass pass = VisitorPass.builder()
                .id(108L)
                .passType(PassType.DELIVERY)
                .status(PassStatus.PENDING)
                .resident(resident)
                .community(community)
                .build();
        when(repo.findById(108L)).thenReturn(Optional.of(pass));
        when(repo.compareAndSwapStatus(eq(108L), eq(PassStatus.PENDING), eq(PassStatus.LEAVE_AT_GATE), any()))
                .thenReturn(1);

        VisitorPassResponse resp = service.leaveAtGate(108L, resident);

        assertThat(resp).isNotNull();
        assertThat(resp.getStatus()).isEqualTo("LEAVE_AT_GATE");
        verify(auditLogRepo).save(argThat(log -> log.getAction().equals("LEAVE_AT_GATE")));
    }

    @Test
    @DisplayName("verifyPassCodeOrOtpOrPhone falls back to group invite when individual pass is absent")
    void verifyPassCodeOrOtpOrPhone_groupInviteFallback() {
        when(repo.findByPassCode("GRP-EVENT-99")).thenReturn(Optional.empty());

        GroupGuest guest1 = GroupGuest.builder()
                .id(1L)
                .name("Sunil Guest")
                .phone("9988776655")
                .isCheckedIn(false)
                .build();

        GroupInvite groupInvite = GroupInvite.builder()
                .id(20L)
                .passCode("GRP-EVENT-99")
                .title("Holi Celebration")
                .host(resident)
                .community(community)
                .guests(List.of(guest1))
                .build();

        when(groupInviteRepo.findByPassCode("GRP-EVENT-99")).thenReturn(Optional.of(groupInvite));

        VisitorPassResponse resp = service.verifyPassCodeOrOtpOrPhone("GRP-EVENT-99", 1L);

        assertThat(resp).isNotNull();
        assertThat(resp.getPassCode()).isEqualTo("GRP-EVENT-99");
        assertThat(resp.getPassType()).isEqualTo("EVENT_GUEST");
        assertThat(resp.getPurpose()).isEqualTo("Holi Celebration");
    }

    @Test
    @DisplayName("approvePass throws 409 InvalidStateTransitionException when concurrent Deny wins the CAS row lock")
    void approvePass_concurrentRaceLost_throwsConflict409() {
        VisitorPass initialPending = VisitorPass.builder()
                .id(301L)
                .visitorName("Rohan Delivery")
                .status(PassStatus.PENDING)
                .resident(resident)
                .community(community)
                .build();
        VisitorPass committedRejected = VisitorPass.builder()
                .id(301L)
                .visitorName("Rohan Delivery")
                .status(PassStatus.REJECTED)
                .resident(resident)
                .community(community)
                .build();

        // First read sees PENDING; after losing CAS row lock (returns 0), reload sees REJECTED from co-resident
        when(repo.findById(301L)).thenReturn(Optional.of(initialPending), Optional.of(committedRejected));
        when(repo.compareAndSwapStatus(eq(301L), eq(PassStatus.PENDING), eq(PassStatus.APPROVED), any()))
                .thenReturn(0);

        assertThatThrownBy(() -> service.approvePass(301L, resident))
                .isInstanceOf(com.manacommunity.api.exception.InvalidStateTransitionException.class)
                .hasMessageContaining("Cannot transition VisitorPass from REJECTED to APPROVED");

        verify(auditLogRepo, never()).save(any());
    }

    @Test
    @DisplayName("rejectPass throws 409 InvalidStateTransitionException if co-resident already APPROVED the walk-in pass")
    void rejectPass_afterAlreadyApproved_throwsConflict409() {
        VisitorPass alreadyApproved = VisitorPass.builder()
                .id(302L)
                .visitorName("Plumber Visit")
                .status(PassStatus.APPROVED)
                .resident(resident)
                .community(community)
                .build();

        when(repo.findById(302L)).thenReturn(Optional.of(alreadyApproved));

        assertThatThrownBy(() -> service.rejectPass(302L, "Co-Resident"))
                .isInstanceOf(com.manacommunity.api.exception.InvalidStateTransitionException.class)
                .hasMessageContaining("Cannot transition VisitorPass from APPROVED to REJECTED");

        verify(repo, never()).compareAndSwapStatus(any(), any(), any(), any());
        verify(auditLogRepo, never()).save(any());
    }

    @Test
    @DisplayName("approvePass commits atomically via compareAndSwapStatus and records audit log")
    void approvePass_casSuccess_transitionsAndWritesAudit() {
        VisitorPass pass = VisitorPass.builder()
                .id(303L)
                .visitorName("Cab Driver")
                .status(PassStatus.PENDING)
                .resident(resident)
                .community(community)
                .build();

        when(repo.findById(303L)).thenReturn(Optional.of(pass));
        when(repo.compareAndSwapStatus(eq(303L), eq(PassStatus.PENDING), eq(PassStatus.APPROVED), any()))
                .thenReturn(1);

        VisitorPassResponse resp = service.approvePass(303L, resident);

        assertThat(resp.getStatus()).isEqualTo("APPROVED");
        verify(auditLogRepo).save(argThat(log -> "RESIDENT_APPROVED".equals(log.getAction())));
    }

    @Test
    @DisplayName("createWalkIn returns existing PENDING pass on idempotencyKey retry without firing duplicate notifications")
    void createWalkIn_idempotentRetry_returnsExistingWithoutDuplicateNotification() {
        VisitorPass existing = VisitorPass.builder()
                .id(401L)
                .passCode("ABCD1234")
                .idempotencyKey("walkin-uuid-001")
                .visitorName("Plumber Ramesh")
                .flatNumber("A-101")
                .status(PassStatus.PENDING)
                .resident(resident)
                .community(community)
                .build();

        when(repo.findByCommunityIdAndIdempotencyKey(1L, "walkin-uuid-001"))
                .thenReturn(Optional.of(existing));

        VisitorPassRequest req = new VisitorPassRequest();
        req.setVisitorName("Plumber Ramesh");
        req.setFlatNumber("A-101");
        req.setIdempotencyKey("walkin-uuid-001");

        VisitorPassResponse resp = service.createWalkIn(req, guard, community);

        assertThat(resp.getId()).isEqualTo(401L);
        assertThat(resp.getStatus()).isEqualTo("PENDING");
        verify(repo, never()).save(any());
        verify(notificationService, never()).createNotification(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("createWalkIn fans out VisitorArrived notifications to ALL active residents of the flat")
    void createWalkIn_flatFanout_notifiesAllFlatResidentsAndPublishesOutbox() {
        AppUser coResident = AppUser.builder().id(11L).fullName("Priya Resident").flatNo("A-101").community(community).build();
        when(userRepo.findByCommunityIdAndFlatNoQuery(1L, "A-101"))
                .thenReturn(List.of(resident, coResident));
        when(repo.save(any(VisitorPass.class))).thenAnswer(inv -> {
            VisitorPass p = inv.getArgument(0);
            p.setId(402L);
            return p;
        });

        VisitorPassRequest req = new VisitorPassRequest();
        req.setVisitorName("Swiggy Delivery");
        req.setFlatNumber("A-101");
        req.setPurpose("Food Order");
        req.setIdempotencyKey("walkin-uuid-002");

        VisitorPassResponse resp = service.createWalkIn(req, guard, community);

        assertThat(resp.getStatus()).isEqualTo("PENDING");
        assertThat(resp.getOtpOnCreation()).isNotBlank();
        verify(domainEventPublisher).publish(eq("VISITOR_PASS"), eq("402"), eq("VisitorArrived"), any(), eq(1L));
        verify(notificationService).createNotification(eq(10L), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
        verify(notificationService).createNotification(eq(11L), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("isResidentAuthorizedForPass permits co-residents of the same flat and rejects unrelated flats")
    void isResidentAuthorizedForPass_allowsSameFlatCoResident() {
        AppUser coResident = AppUser.builder().id(11L).fullName("Co-Resident").flatNo(" a-101 ").community(community).build();
        AppUser neighbor = AppUser.builder().id(12L).fullName("Neighbor").flatNo("B-202").community(community).build();

        VisitorPassResponse pass = VisitorPassResponse.builder()
                .id(500L)
                .residentId(10L)
                .communityId(1L)
                .flatNumber("A-101")
                .build();

        assertThat(service.isResidentAuthorizedForPass(coResident, pass)).isTrue();
        assertThat(service.isResidentAuthorizedForPass(neighbor, pass)).isFalse();
    }

    @Test
    @DisplayName("leaveAtGate transitions to LEAVE_AT_GATE and provisions ParcelRecord with 24h pickup OTP")
    void leaveAtGate_createsParcelRecordWithOtpAndNotifiesResident() {
        ParcelService parcelService = mock(ParcelService.class);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "parcelService", parcelService);

        VisitorPass pending = VisitorPass.builder()
                .id(601L)
                .passCode("DELIV601")
                .visitorName("Amazon Courier")
                .flatNumber("A-101")
                .passType(PassType.DELIVERY)
                .status(PassStatus.PENDING)
                .resident(resident)
                .community(community)
                .build();

        when(repo.findById(601L)).thenReturn(Optional.of(pending));
        when(repo.compareAndSwapStatus(eq(601L), eq(PassStatus.PENDING), eq(PassStatus.LEAVE_AT_GATE), any()))
                .thenReturn(1);
        when(parcelService.receiveParcelWithOtp(eq(1L), eq("A-101"), eq(10L), eq("Amazon Courier"), eq("DELIV601"), eq(10L), isNull()))
                .thenReturn(ParcelRecord.builder()
                        .id(88L)
                        .communityId(1L)
                        .flatNo("A-101")
                        .collectionOtp("739201")
                        .status("AWAITING_PICKUP")
                        .build());

        VisitorPassResponse resp = service.leaveAtGate(601L, resident);

        assertThat(resp.getStatus()).isEqualTo("LEAVE_AT_GATE");
        assertThat(resp.getParcelRecordId()).isEqualTo(88L);
        assertThat(resp.getParcelCollectionOtp()).isEqualTo("739201");
        verify(notificationService).createNotification(eq(10L), any(), any(), eq("Parcel Left at Gate Desk"),
                contains("739201"), any(), any(), eq(601L), any(), any(), eq(1L));
    }

    @Test
    @DisplayName("createWalkIn honors defaultLeaveAtGate preference and denyPass transitions to DENIED")
    void createWalkIn_defaultLeaveAtGate_and_denyPassTransitionsToDenied() {
        ResidentDeliveryPreferenceRepository prefRepo = mock(ResidentDeliveryPreferenceRepository.class);
        ParcelService parcelService = mock(ParcelService.class);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "deliveryPreferenceRepo", prefRepo);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "parcelService", parcelService);

        when(userRepo.findByCommunityIdAndFlatNoQuery(1L, "A-101")).thenReturn(List.of(resident));
        when(prefRepo.findByCommunityIdAndFlatNumber(1L, "A-101"))
                .thenReturn(Optional.of(ResidentDeliveryPreference.builder()
                        .communityId(1L).flatNumber("A-101").defaultLeaveAtGate(true).build()));
        when(repo.save(any(VisitorPass.class))).thenAnswer(inv -> {
            VisitorPass p = inv.getArgument(0);
            p.setId(602L);
            return p;
        });
        when(repo.findById(602L)).thenAnswer(inv -> Optional.of(VisitorPass.builder()
                .id(602L).passCode("DELIV602").visitorName("Blinkit Rider").flatNumber("A-101")
                .passType(PassType.DELIVERY).status(PassStatus.PENDING).resident(resident).community(community).build()));
        when(repo.compareAndSwapStatus(eq(602L), eq(PassStatus.PENDING), eq(PassStatus.LEAVE_AT_GATE), any()))
                .thenReturn(1);
        when(parcelService.receiveParcelWithOtp(eq(1L), eq("A-101"), eq(10L), eq("Blinkit Rider"), eq("DELIV602"), eq(10L), isNull()))
                .thenReturn(ParcelRecord.builder().id(89L).collectionOtp("551199").build());

        VisitorPassRequest req = new VisitorPassRequest();
        req.setVisitorName("Blinkit Rider");
        req.setFlatNumber("A-101");
        req.setPassType("DELIVERY");
        req.setIdempotencyKey("walkin-auto-lag");

        VisitorPassResponse autoResp = service.createWalkIn(req, guard, community);
        assertThat(autoResp.getStatus()).isEqualTo("LEAVE_AT_GATE");
        assertThat(autoResp.getParcelCollectionOtp()).isEqualTo("551199");
        assertThat(autoResp.getOtpOnCreation()).isNotBlank();

        VisitorPass pendingWalkIn = VisitorPass.builder()
                .id(603L).passCode("WALK603").visitorName("Unwanted Sales").flatNumber("A-101")
                .status(PassStatus.PENDING).resident(resident).community(community).build();
        when(repo.findById(603L)).thenReturn(Optional.of(pendingWalkIn));
        when(repo.compareAndSwapStatus(eq(603L), eq(PassStatus.PENDING), eq(PassStatus.DENIED), any()))
                .thenReturn(1);

        VisitorPassResponse denied = service.denyPass(603L, "Asha Resident");
        assertThat(denied.getStatus()).isEqualTo("DENIED");
    }

    @Test
    @DisplayName("verifyDeliveryOtp locks after 5 failed attempts and autoDetectOverstays triggers SLA alerts")
    void verifyDeliveryOtp_lockout_and_overstayAlert() {
        OverstaySlaEngineService slaEngine = new OverstaySlaEngineService();
        org.springframework.test.util.ReflectionTestUtils.setField(service, "overstaySlaEngineService", slaEngine);

        VisitorPass deliveryPending = VisitorPass.builder()
                .id(701L).passCode("OTP701").flatNumber("A-101").passType(PassType.DELIVERY)
                .status(PassStatus.PENDING).otpAttempts(4).resident(resident).community(community).build();
        when(repo.findByOtpHash(anyString())).thenReturn(Optional.empty());
        when(repo.findByCommunityIdAndStatusOrderByCreatedAtDesc(1L, PassStatus.PENDING))
                .thenReturn(List.of(deliveryPending));
        when(repo.save(any(VisitorPass.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> service.verifyDeliveryOtp("A-101", "000000", guard, community))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("locked after 5 failed attempts");
        assertThat(deliveryPending.getOtpAttempts()).isEqualTo(5);

        VisitorPass overstayedCourier = VisitorPass.builder()
                .id(702L).passCode("OVER702").visitorName("Zomato Rider").flatNumber("A-101")
                .passType(PassType.DELIVERY).status(PassStatus.CHECKED_IN)
                .checkedInAt(LocalDateTime.now().minusMinutes(60))
                .resident(resident).community(community).build();
        when(repo.findAll()).thenReturn(List.of(overstayedCourier));
        when(repo.findById(702L)).thenReturn(Optional.of(overstayedCourier));

        service.autoDetectOverstays();
        verify(notificationService).createNotification(eq(10L), any(), any(), eq("Visitor Overstay Alert"),
                contains("45-min SLA"), any(), any(), eq(702L), any(), any(), eq(1L));
    }
}

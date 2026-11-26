package com.manacommunity.api.unit;

import com.manacommunity.api.model.Community;
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
}

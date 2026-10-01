package com.manacommunity.api.unit.notification.orchestrator;

import com.manacommunity.api.email.EmailService;
import com.manacommunity.api.notification.orchestrator.*;
import com.manacommunity.api.notification.orchestrator.NotificationDtos.*;
import com.manacommunity.api.notification.orchestrator.NotificationEnums.*;
import com.manacommunity.api.notification.orchestrator.engine.NotificationPreferenceEngine;
import com.manacommunity.api.notification.orchestrator.provider.WhatsAppProvider;
import com.manacommunity.api.notification.service.SmsService;
import com.manacommunity.api.service.ExpoPushService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UnifiedNotificationService Unit Tests")
public class UnifiedNotificationServiceTest {

    @Mock
    private AppUserRepository userRepository;

    @Mock
    private NotificationPreferenceRepository preferenceRepository;

    @Mock
    private NotificationAuditLogRepository auditLogRepository;

    @Spy
    private NotificationPreferenceEngine preferenceEngine = new NotificationPreferenceEngine();

    @Mock
    private ExpoPushService pushService;

    @Mock
    private EmailService emailService;

    @Mock
    private SmsService smsService;

    @Mock
    private WhatsAppProvider whatsAppProvider;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private UnifiedNotificationServiceImpl service;

    private AppUser mockUser;

    @BeforeEach
    void setUp() {
        mockUser = AppUser.builder()
                .id(101L)
                .fullName("Ravi Kumar")
                .email("ravi@example.com")
                .phone("+919876543210")
                .build();
    }

    @Test
    @DisplayName("Should deliver critical SOS across all channels")
    void testCriticalEmergencyAllChannels() {
        when(userRepository.findById(101L)).thenReturn(Optional.of(mockUser));
        when(whatsAppProvider.sendTemplateMessage(any())).thenReturn(
                NotificationDtos.WhatsAppTemplateResponse.builder().success(true).providerMessageId("wam_123").build());
        when(smsService.send(any())).thenReturn(999L);

        UnifiedNotificationRequest request = UnifiedNotificationRequest.builder()
                .recipientUserId(101L)
                .category(NotificationCategory.EMERGENCY_SOS)
                .priority(NotificationPriority.CRITICAL)
                .strategy(DeliveryStrategy.ALL_CHANNELS)
                .title("FIRE ALARM TOWER B")
                .body("Evacuate immediately via stairs")
                .build();

        UnifiedNotificationResult result = service.orchestrate(request);

        assertNotNull(result);
        assertTrue(result.isOverallDelivered());
        assertEquals(5, result.getChannelReports().size()); // IN_APP, PUSH, SMS, WHATSAPP, EMAIL
        verify(pushService).sendToUser(eq(101L), anyString(), anyString(), any(), anyString());
        verify(emailService).send(any());
        verify(smsService).send(any());
        verify(whatsAppProvider).sendTemplateMessage(any());
    }

    @Test
    @DisplayName("Should successfully handle fallback cascade")
    void testFallbackCascade() {
        when(userRepository.findById(101L)).thenReturn(Optional.of(mockUser));

        UnifiedNotificationRequest request = UnifiedNotificationRequest.builder()
                .recipientUserId(101L)
                .category(NotificationCategory.GATE_ACCESS)
                .priority(NotificationPriority.NORMAL)
                .strategy(DeliveryStrategy.FALLBACK_CASCADE)
                .title("Visitor At Gate")
                .body("Courier delivery waiting")
                .build();

        UnifiedNotificationResult result = service.orchestrate(request);

        assertNotNull(result);
        assertTrue(result.isOverallDelivered());
        // First channel PUSH succeeds -> loop breaks out
        assertEquals(1, result.getChannelReports().size());
        assertEquals(NotificationChannel.PUSH, result.getChannelReports().get(0).getChannel());
    }
}

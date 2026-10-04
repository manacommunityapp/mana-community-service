package com.manacommunity.api.unit.access.biometric;

import com.manacommunity.api.access.biometric.*;
import com.manacommunity.api.access.biometric.BiometricEnums.*;
import com.manacommunity.api.access.biometric.dto.BiometricDtos.*;
import com.manacommunity.api.access.biometric.engine.TurnstileAccessRuleEngine;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BiometricTurnstileService Unit Tests")
public class BiometricTurnstileServiceTest {

    @Mock
    private BiometricTurnstileRepository turnstileRepository;

    @Mock
    private BiometricUserEnrollmentRepository enrollmentRepository;

    @Mock
    private BiometricAccessLogRepository accessLogRepository;

    @Spy
    private TurnstileAccessRuleEngine ruleEngine = new TurnstileAccessRuleEngine();

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private BiometricTurnstileServiceImpl service;

    private BiometricTurnstile mockTurnstile;
    private BiometricUserEnrollment mockEnrollment;

    @BeforeEach
    void setUp() {
        Community comm = Community.builder().id(1L).name("Mana Tower").build();
        mockTurnstile = BiometricTurnstile.builder()
                .id(10L)
                .turnstileIdentifier("TS-01")
                .turnstileName("North Pedestrian Gate")
                .community(comm)
                .status(TurnstileStatus.ONLINE)
                .relayUnlockMs(3000)
                .build();

        AppUser user = AppUser.builder().id(55L).fullName("Sunita Devi").build();
        mockEnrollment = BiometricUserEnrollment.builder()
                .id(1L)
                .community(comm)
                .user(user)
                .personName("Sunita Devi")
                .personType(BiometricPersonType.DOMESTIC_STAFF)
                .faceEmbeddingHash("hash_sunita_123")
                .enrollmentStatus(EnrollmentStatus.ENROLLED)
                .timeWindowStart("06:00")
                .timeWindowEnd("23:00")
                .allowedDays("MON,TUE,WED,THU,FRI,SAT,SUN")
                .build();
    }

    @Test
    @DisplayName("VerifyFace unlocks relay and broadcasts STOMP event")
    void testVerifyFace() {
        when(turnstileRepository.findByTurnstileIdentifier("TS-01")).thenReturn(Optional.of(mockTurnstile));
        when(enrollmentRepository.findByFaceEmbeddingHash("hash_sunita_123")).thenReturn(Optional.of(mockEnrollment));

        VerifyFaceRequest req = VerifyFaceRequest.builder()
                .turnstileIdentifier("TS-01")
                .faceEmbeddingHash("hash_sunita_123")
                .confidenceScore(new BigDecimal("0.97"))
                .build();

        VerifyFaceResult result = service.verifyFace(req);

        assertNotNull(result);
        assertEquals(AccessDecision.GRANTED_OPEN, result.getDecision());
        assertTrue(result.isRelayUnlock());
        assertEquals("Sunita Devi", result.getPersonName());
        verify(accessLogRepository).save(any());
        verify(messagingTemplate).convertAndSend(anyString(), any(Object.class));
    }
}

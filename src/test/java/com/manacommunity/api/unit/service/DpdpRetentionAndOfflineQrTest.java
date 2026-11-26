package com.manacommunity.api.unit.service;

import com.manacommunity.api.privacy.DataRetentionPolicy;
import com.manacommunity.api.privacy.DataRetentionPolicyRepository;
import com.manacommunity.api.privacy.DataRetentionService;
import com.manacommunity.api.privacy.PrivacyAuditService;
import com.manacommunity.api.security.AuditService;
import com.manacommunity.api.marketplace.repository.MarketOrderRepository;
import com.manacommunity.api.visitor.entity.VisitorPass.PassStatus;
import com.manacommunity.api.visitor.repository.VisitorPassRepository;
import com.manacommunity.api.visitor.security.OfflineQrTokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.*;
import java.util.List;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DpdpRetentionAndOfflineQrTest {

    @Mock private DataRetentionPolicyRepository policyRepo;
    @Mock private VisitorPassRepository visitorPassRepo;
    @Mock private MarketOrderRepository marketOrderRepo;
    @Mock private PrivacyAuditService privacyAuditService;
    @Mock private AuditService auditService;

    @InjectMocks
    private DataRetentionService retentionService;

    @Test
    @DisplayName("DPDP enforcement processes 2000-row keyset batches without ever calling findAll()")
    void dpdpRetentionProcessesKeysetBatchesWithoutFindAll() {
        DataRetentionPolicy policy = DataRetentionPolicy.builder()
                .id(1L)
                .communityId(7L)
                .dataCategory("VISITOR_LOGS")
                .retentionPeriodDays(30)
                .actionOnExpiry("ANONYMIZE")
                .isActive(true)
                .build();

        List<Long> firstPage = LongStream.rangeClosed(1L, 2000L).boxed().toList();
        List<Long> secondPage = List.of(2001L, 2002L, 2003L);

        when(visitorPassRepo.findRetentionCandidateIds(eq(7L), any(), anyList(), eq(0L), eq(false), any()))
                .thenReturn(firstPage);
        when(visitorPassRepo.findRetentionCandidateIds(eq(7L), any(), anyList(), eq(2000L), eq(false), any()))
                .thenReturn(secondPage);
        when(visitorPassRepo.anonymizePassBatch(firstPage)).thenReturn(2000);
        when(visitorPassRepo.anonymizePassBatch(secondPage)).thenReturn(3);

        int affected = retentionService.enforceVisitorLogRetention(policy);

        assertThat(affected).isEqualTo(2003);
        verify(visitorPassRepo, never()).findAll();
        verify(visitorPassRepo).anonymizePassBatch(firstPage);
        verify(visitorPassRepo).anonymizePassBatch(secondPage);
        verify(privacyAuditService).record(anyString(), eq("VISITOR_PASS_BATCH"), eq("COUNT:2003"), eq(7L), anyString());
    }

    @Test
    @DisplayName("Ed25519 OfflineQrTokenService validates signed QR offline and blocks tampered, cross-tenant, and expired tokens")
    void offlineQrTokenVerifiesSignatureAndBlocksTampering() throws Exception {
        KeyPair kp = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        Instant fixedStart = Instant.parse("2026-11-27T01:00:00Z");
        Clock signClock = Clock.fixed(fixedStart, ZoneOffset.UTC);

        OfflineQrTokenService signer = new OfflineQrTokenService(kp, signClock);
        String publicKeyB64 = signer.getPublicKeyBase64();

        String token = signer.signToken(10L, "MANA-8821", "A-302", 600L);

        // Valid offline verification with only public key
        var validRes = OfflineQrTokenService.verifyWithPublicKeyBase64(token, publicKeyB64, 10L, signClock);
        assertThat(validRes.isValid()).isTrue();
        assertThat(validRes.getPassCode()).isEqualTo("MANA-8821");
        assertThat(validRes.getFlatNumber()).isEqualTo("A-302");
        assertThat(validRes.getJti()).isNotBlank();

        // Cross-community IDOR rejection (Community 99 scanner refuses Community 10 pass)
        var crossTenant = OfflineQrTokenService.verifyWithPublicKeyBase64(token, publicKeyB64, 99L, signClock);
        assertThat(crossTenant.isValid()).isFalse();
        assertThat(crossTenant.getReason()).contains("Cross-community");

        // Tampered payload/signature rejection
        String tampered = token.substring(0, token.length() - 3) + "AAA";
        var forged = OfflineQrTokenService.verifyWithPublicKeyBase64(tampered, publicKeyB64, 10L, signClock);
        assertThat(forged.isValid()).isFalse();

        // Expired QR rejection (after TTL)
        Clock expiredClock = Clock.fixed(fixedStart.plusSeconds(605L), ZoneOffset.UTC);
        var expired = OfflineQrTokenService.verifyWithPublicKeyBase64(token, publicKeyB64, 10L, expiredClock);
        assertThat(expired.isValid()).isFalse();
        assertThat(expired.getReason()).contains("expired");
    }
}

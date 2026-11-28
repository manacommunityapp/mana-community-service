package com.manacommunity.api.audit;

import com.manacommunity.api.model.AuditLog;
import com.manacommunity.api.repository.AuditLogRepository;
import com.manacommunity.api.security.AuditAction;
import com.manacommunity.api.security.AuditModule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TamperResistantAuditLogTest {

    @Mock private AuditLogRepository auditLogRepo;

    private TamperResistantAuditService auditService;
    private final List<AuditLog> ledger = new ArrayList<>();

    @BeforeEach
    void setUp() {
        ledger.clear();
        auditService = new TamperResistantAuditService(
                auditLogRepo, "test-audit-hmac-secret-2026-deterministic");

        when(auditLogRepo.findTopByTenantIdOrderBySequenceNumberDesc(anyString()))
                .thenAnswer(inv -> {
                    String tid = inv.getArgument(0);
                    return ledger.stream()
                            .filter(a -> tid.equals(a.getTenantId()))
                            .max(Comparator.comparingLong(AuditLog::getSequenceNumber));
                });
        when(auditLogRepo.save(any(AuditLog.class))).thenAnswer(inv -> {
            AuditLog entry = inv.getArgument(0);
            entry.setId((long) (ledger.size() + 1));
            ledger.add(entry);
            return entry;
        });
    }

    @AfterEach
    void tearDown() {
        AuditContextHolder.clearContext();
    }

    @Test
    void recordsImmutableActorDeviceFingerprintIpBeforeAfterDiffAndDetectsTampering() {
        AuditContextHolder.setContext(AuditContext.builder()
                .tenantId("SOC-412")
                .userId(901L)
                .username("treasurer.anil")
                .userRole("TREASURER")
                .ipAddress("203.0.113.45")
                .deviceInfo("Android-Pixel8-FP:ab12cd34")
                .userAgent("ManaCommunityApp/5.2.0")
                .correlationId("trace-audit-8821")
                .build());

        // Sensitive Action 1: Penalty waiver with before/after JSON diff
        AuditLog waiver = auditService.record(
                "SOC-412",
                AuditAction.BILL_MODIFIED,
                AuditModule.FINANCE,
                "PENALTY_WAIVER",
                "INV-9001",
                "{\"penaltyPaise\":250000,\"status\":\"OVERDUE\"}",
                "{\"penaltyPaise\":0,\"status\":\"WAIVED\"}",
                "mana-community-service",
                "POST /api/v1/finance/invoices/INV-9001/waive"
        );

        assertThat(waiver).isNotNull();
        assertThat(waiver.getSequenceNumber()).isEqualTo(1L);
        assertThat(waiver.getUserId()).isEqualTo(901L);
        assertThat(waiver.getIpAddress()).isEqualTo("203.0.113.45");
        assertThat(waiver.getDeviceInfo()).isEqualTo("Android-Pixel8-FP:ab12cd34");
        assertThat(waiver.getOldValue()).contains("\"penaltyPaise\":250000");
        assertThat(waiver.getNewValue()).contains("\"status\":\"WAIVED\"");
        assertThat(waiver.getCreatedAt()).isNotNull();
        assertThat(waiver.getRecordHash()).hasSize(64);
        assertThat(waiver.getSignature()).hasSize(64);

        // Sensitive Action 2: Vendor blacklisting links prevHash to Action 1
        AuditLog blacklist = auditService.record(
                "SOC-412",
                AuditAction.WATCHLIST_APPROVED,
                AuditModule.SAFETY_SECURITY,
                "VENDOR_BLACKLIST",
                "VND-77",
                "{\"blacklisted\":false}",
                "{\"blacklisted\":true,\"reason\":\"Fake KYC\"}",
                "mana-community-service",
                "POST /api/v1/vendors/VND-77/blacklist"
        );

        assertThat(blacklist.getSequenceNumber()).isEqualTo(2L);
        assertThat(blacklist.getPrevHash()).isEqualTo(waiver.getRecordHash());

        // Recomputing canonical payload on untouched Waiver reproduces exact recordHash
        String untouchedCanonical = TamperResistantAuditService.computeCanonicalPayload(
                waiver.getTenantId(), waiver.getSequenceNumber(), waiver.getCreatedAt(),
                waiver.getUserId(), waiver.getUsername(), waiver.getAction(), waiver.getModule(),
                waiver.getEntityName(), waiver.getEntityId(), waiver.getOldValue(), waiver.getNewValue(),
                waiver.getCorrelationId(), waiver.getIpAddress());
        String verifiedHash = TamperResistantAuditService.hmacSha256Hex(
                "test-audit-hmac-secret-2026-deterministic",
                waiver.getPrevHash() + "::" + untouchedCanonical);
        assertThat(verifiedHash).isEqualTo(waiver.getRecordHash());

        // Post-hoc DB tampering of newValue breaks HMAC verification immediately
        String tamperedCanonical = TamperResistantAuditService.computeCanonicalPayload(
                waiver.getTenantId(), waiver.getSequenceNumber(), waiver.getCreatedAt(),
                waiver.getUserId(), waiver.getUsername(), waiver.getAction(), waiver.getModule(),
                waiver.getEntityName(), waiver.getEntityId(), waiver.getOldValue(),
                "{\"penaltyPaise\":0,\"status\":\"TAMPERED\"}",
                waiver.getCorrelationId(), waiver.getIpAddress());
        String tamperedHash = TamperResistantAuditService.hmacSha256Hex(
                "test-audit-hmac-secret-2026-deterministic",
                waiver.getPrevHash() + "::" + tamperedCanonical);
        assertThat(tamperedHash).isNotEqualTo(waiver.getRecordHash());
    }
}

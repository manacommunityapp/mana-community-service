package com.manacommunity.api.unit;

import com.manacommunity.api.privacy.DataRetentionPolicy;
import com.manacommunity.api.privacy.DataRetentionPolicyRepository;
import com.manacommunity.api.privacy.DataRetentionService;
import com.manacommunity.api.privacy.PrivacyAuditService;
import com.manacommunity.api.privacy.dto.DataRetentionPolicyDto;
import com.manacommunity.api.security.AuditService;
import com.manacommunity.api.visitor.entity.VisitorPass;
import com.manacommunity.api.visitor.repository.VisitorPassRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link DataRetentionService} — policy retrieval and update logic.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DataRetentionService")
class DataRetentionServiceTest {

    @Mock
    private DataRetentionPolicyRepository policyRepo;
    @Mock
    private VisitorPassRepository visitorPassRepo;
    @Mock
    private PrivacyAuditService privacyAuditService;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private DataRetentionService service;

    @Nested
    @DisplayName("updatePolicy")
    class UpdatePolicy {

        @Test
        @DisplayName("updates retention period and action")
        void updatesRetentionPeriodAndAction() {
            DataRetentionPolicy existing = DataRetentionPolicy.builder()
                    .id(1L).dataCategory("VISITOR_LOGS")
                    .retentionPeriodDays(90).actionOnExpiry("ANONYMIZE").isActive(true)
                    .build();

            when(policyRepo.findById(1L)).thenReturn(Optional.of(existing));
            when(policyRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            DataRetentionPolicyDto update = DataRetentionPolicyDto.builder()
                    .retentionPeriodDays(180)
                    .actionOnExpiry("DELETE")
                    .build();

            DataRetentionPolicyDto result = service.updatePolicy(1L, update);

            assertThat(result.getRetentionPeriodDays()).isEqualTo(180);
            assertThat(result.getActionOnExpiry()).isEqualTo("DELETE");
        }

        @Test
        @DisplayName("action is uppercased when saved")
        void actionIsUppercased() {
            DataRetentionPolicy existing = DataRetentionPolicy.builder()
                    .id(2L).dataCategory("NOTIFICATIONS")
                    .retentionPeriodDays(60).actionOnExpiry("DELETE").isActive(true)
                    .build();

            when(policyRepo.findById(2L)).thenReturn(Optional.of(existing));
            when(policyRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            DataRetentionPolicyDto update = DataRetentionPolicyDto.builder()
                    .actionOnExpiry("anonymize")
                    .build();

            DataRetentionPolicyDto result = service.updatePolicy(2L, update);

            assertThat(result.getActionOnExpiry()).isEqualTo("ANONYMIZE");
        }

        @Test
        @DisplayName("audit is recorded on update")
        void auditIsRecorded() {
            DataRetentionPolicy existing = DataRetentionPolicy.builder()
                    .id(3L).dataCategory("AUDIT_LOGS").retentionPeriodDays(365)
                    .actionOnExpiry("ARCHIVE").isActive(true).build();

            when(policyRepo.findById(3L)).thenReturn(Optional.of(existing));
            when(policyRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.updatePolicy(3L, DataRetentionPolicyDto.builder().isActive(false).build());

            verify(auditService).record(any(), any(), anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("getPolicies")
    class GetPolicies {

        @Test
        @DisplayName("returns policies for given community including global (null community)")
        void returnsPoliciesForCommunity() {
            DataRetentionPolicy global = DataRetentionPolicy.builder()
                    .id(1L).communityId(null).dataCategory("VISITOR_LOGS")
                    .retentionPeriodDays(90).actionOnExpiry("ANONYMIZE").isActive(true).build();
            DataRetentionPolicy community = DataRetentionPolicy.builder()
                    .id(2L).communityId(10L).dataCategory("NOTIFICATIONS")
                    .retentionPeriodDays(60).actionOnExpiry("DELETE").isActive(true).build();

            when(policyRepo.findByCommunityIdOrCommunityIdIsNull(10L))
                    .thenReturn(List.of(global, community));

            List<DataRetentionPolicyDto> policies = service.getPolicies(10L);

            assertThat(policies).hasSize(2);
            assertThat(policies).extracting(DataRetentionPolicyDto::getDataCategory)
                    .containsExactlyInAnyOrder("VISITOR_LOGS", "NOTIFICATIONS");
        }
    }
}

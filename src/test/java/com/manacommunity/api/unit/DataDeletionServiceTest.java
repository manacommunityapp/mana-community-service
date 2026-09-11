package com.manacommunity.api.unit;

import com.manacommunity.api.privacy.DataDeletionRequest;
import com.manacommunity.api.privacy.DataDeletionRequest.DeletionStatus;
import com.manacommunity.api.privacy.DataDeletionRequestRepository;
import com.manacommunity.api.privacy.DataDeletionService;
import com.manacommunity.api.privacy.PrivacyAuditService;
import com.manacommunity.api.privacy.UserPrivacySettingsRepository;
import com.manacommunity.api.privacy.dto.DataDeletionRequestDto;
import com.manacommunity.api.exception.InvalidInputException;
import com.manacommunity.api.security.AuditService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import com.manacommunity.api.user.repository.FamilyMemberRepository;
import com.manacommunity.api.user.repository.UserSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link DataDeletionService}.
 * Verifies the full deletion workflow and PII anonymization logic.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DataDeletionService")
class DataDeletionServiceTest {

    @Mock
    private DataDeletionRequestRepository requestRepo;
    @Mock
    private AppUserRepository userRepo;
    @Mock
    private FamilyMemberRepository familyMemberRepo;
    @Mock
    private UserSessionRepository userSessionRepo;
    @Mock
    private UserPrivacySettingsRepository privacySettingsRepo;
    @Mock
    private PrivacyAuditService privacyAuditService;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private DataDeletionService service;

    private AppUser targetUser;

    @BeforeEach
    void setUp() {
        targetUser = new AppUser();
        targetUser.setId(42L);
        targetUser.setFullName("Ramesh Kumar");
        targetUser.setEmail("ramesh@example.com");
        targetUser.setPhone("9876543210");
        targetUser.setGovtIdNumber("1234 5678 9012");
        targetUser.setGovtIdType("AADHAAR");
    }

    @Nested
    @DisplayName("submitRequest")
    class SubmitRequest {

        @Test
        @DisplayName("creates a deletion request with PENDING status")
        void createsPendingRequest() {
            when(requestRepo.findFirstByUserIdAndStatusInOrderByRequestedAtDesc(eq(42L), anyList()))
                    .thenReturn(Optional.empty());
            when(requestRepo.save(any())).thenAnswer(inv -> {
                DataDeletionRequest req = inv.getArgument(0);
                req.setId(1L);
                return req;
            });

            DataDeletionRequestDto dto = service.submitRequest(42L, "I want to leave", 10L);

            assertThat(dto.getStatus()).isEqualTo("PENDING");
            assertThat(dto.getReason()).isEqualTo("I want to leave");

            ArgumentCaptor<DataDeletionRequest> captor = ArgumentCaptor.forClass(DataDeletionRequest.class);
            verify(requestRepo).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(DeletionStatus.PENDING);
            assertThat(captor.getValue().getVerificationToken()).isNotBlank();
            assertThat(captor.getValue().getVerificationExpiry()).isAfter(LocalDateTime.now());
        }

        @Test
        @DisplayName("rejects duplicate pending request")
        void rejectsDuplicatePending() {
            DataDeletionRequest existing = DataDeletionRequest.builder()
                    .id(5L).userId(42L).status(DeletionStatus.PENDING).build();
            when(requestRepo.findFirstByUserIdAndStatusInOrderByRequestedAtDesc(eq(42L), anyList()))
                    .thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> service.submitRequest(42L, "Another request", 10L))
                    .isInstanceOf(InvalidInputException.class)
                    .hasMessageContaining("active data deletion request already exists");

            verify(requestRepo, never()).save(any());
        }
    }

    @Nested
    @DisplayName("processDeletion")
    class ProcessDeletion {

        @Test
        @DisplayName("anonymizes user PII on execution")
        void anonymizesUserPii() {
            DataDeletionRequest request = DataDeletionRequest.builder()
                    .id(1L).userId(42L).communityId(10L).status(DeletionStatus.PENDING).build();

            when(requestRepo.findById(1L)).thenReturn(Optional.of(request));
            when(userRepo.findById(42L)).thenReturn(Optional.of(targetUser));
            when(familyMemberRepo.findByUserIdOrderByCreatedAtAsc(42L)).thenReturn(List.of());
            when(userSessionRepo.findByUserIdAndStatus(42L, "ACTIVE")).thenReturn(List.of());
            when(userSessionRepo.findByUserIdAndStatus(42L, "LOGGED_IN")).thenReturn(List.of());
            when(privacySettingsRepo.findByUserId(42L)).thenReturn(Optional.empty());
            when(requestRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.processDeletion(1L, 99L, "Processed by admin");

            ArgumentCaptor<AppUser> userCaptor = ArgumentCaptor.forClass(AppUser.class);
            verify(userRepo).save(userCaptor.capture());

            AppUser anonymized = userCaptor.getValue();
            assertThat(anonymized.getFullName()).startsWith("Deactivated User");
            assertThat(anonymized.getEmail()).endsWith("@deleted.manacommunity.com");
            assertThat(anonymized.getGovtIdNumber()).isNull();
            assertThat(anonymized.getGovtIdType()).isNull();
            assertThat(anonymized.getDateOfBirth()).isNull();
            assertThat(anonymized.getIsActive()).isFalse();
        }

        @Test
        @DisplayName("marks deletion request as COMPLETED after execution")
        void marksRequestCompleted() {
            DataDeletionRequest request = DataDeletionRequest.builder()
                    .id(1L).userId(42L).communityId(10L).status(DeletionStatus.PENDING).build();

            when(requestRepo.findById(1L)).thenReturn(Optional.of(request));
            when(userRepo.findById(42L)).thenReturn(Optional.of(targetUser));
            when(familyMemberRepo.findByUserIdOrderByCreatedAtAsc(42L)).thenReturn(List.of());
            when(userSessionRepo.findByUserIdAndStatus(anyLong(), anyString())).thenReturn(List.of());
            when(privacySettingsRepo.findByUserId(42L)).thenReturn(Optional.empty());
            when(requestRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            DataDeletionRequestDto result = service.processDeletion(1L, 99L, "notes");

            assertThat(result.getStatus()).isEqualTo("COMPLETED");
            assertThat(result.getProcessedBy()).isEqualTo(99L);
            assertThat(result.getNotes()).isEqualTo("notes");
        }

        @Test
        @DisplayName("anonymized email never contains the original email")
        void anonymizedEmailDoesNotContainOriginal() {
            DataDeletionRequest request = DataDeletionRequest.builder()
                    .id(2L).userId(42L).communityId(10L).status(DeletionStatus.PENDING).build();

            when(requestRepo.findById(2L)).thenReturn(Optional.of(request));
            when(userRepo.findById(42L)).thenReturn(Optional.of(targetUser));
            when(familyMemberRepo.findByUserIdOrderByCreatedAtAsc(42L)).thenReturn(List.of());
            when(userSessionRepo.findByUserIdAndStatus(anyLong(), anyString())).thenReturn(List.of());
            when(privacySettingsRepo.findByUserId(42L)).thenReturn(Optional.empty());
            when(requestRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.processDeletion(2L, 99L, null);

            ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
            verify(userRepo).save(captor.capture());

            String newEmail = captor.getValue().getEmail();
            assertThat(newEmail).doesNotContain("ramesh@example.com");
            assertThat(newEmail).doesNotContain("ramesh");
        }

        @Test
        @DisplayName("throws InvalidInputException if request is already COMPLETED")
        void rejectsAlreadyCompletedRequest() {
            DataDeletionRequest completed = DataDeletionRequest.builder()
                    .id(3L).userId(42L).status(DeletionStatus.COMPLETED).build();
            when(requestRepo.findById(3L)).thenReturn(Optional.of(completed));

            assertThatThrownBy(() -> service.processDeletion(3L, 99L, null))
                    .isInstanceOf(InvalidInputException.class)
                    .hasMessageContaining("already been processed");
        }
    }

    @Nested
    @DisplayName("rejectRequest")
    class RejectRequest {

        @Test
        @DisplayName("marks request as REJECTED with admin notes")
        void marksRequestRejected() {
            DataDeletionRequest request = DataDeletionRequest.builder()
                    .id(4L).userId(42L).communityId(10L).status(DeletionStatus.PENDING).build();

            when(requestRepo.findById(4L)).thenReturn(Optional.of(request));
            when(requestRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            DataDeletionRequestDto result = service.rejectRequest(4L, 99L, "Insufficient evidence");

            assertThat(result.getStatus()).isEqualTo("REJECTED");
            assertThat(result.getNotes()).isEqualTo("Insufficient evidence");

            // No user PII should be modified
            verify(userRepo, never()).save(any());
        }
    }
}

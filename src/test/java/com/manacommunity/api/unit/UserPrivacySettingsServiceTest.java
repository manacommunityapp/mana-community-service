package com.manacommunity.api.unit;

import com.manacommunity.api.privacy.UserPrivacySettings;
import com.manacommunity.api.privacy.UserPrivacySettingsRepository;
import com.manacommunity.api.privacy.UserPrivacySettingsService;
import com.manacommunity.api.privacy.PrivacyAuditService;
import com.manacommunity.api.privacy.dto.UserPrivacySettingsDto;
import com.manacommunity.api.security.AuditService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link UserPrivacySettingsService}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserPrivacySettingsService")
class UserPrivacySettingsServiceTest {

    @Mock
    private UserPrivacySettingsRepository repo;
    @Mock
    private PrivacyAuditService privacyAuditService;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private UserPrivacySettingsService service;

    @Nested
    @DisplayName("getSettings")
    class GetSettings {

        @Test
        @DisplayName("returns saved settings when they exist")
        void returnsSavedSettings() {
            UserPrivacySettings saved = UserPrivacySettings.builder()
                    .id(1L).userId(10L)
                    .showPhoneToNeighbours(true)
                    .showEmailToNeighbours(false)
                    .showFlatInDirectory(true)
                    .showFamilyMembers(false)
                    .allowMarketplaceContact(true)
                    .allowEventTagging(false)
                    .activityVisibility("PRIVATE")
                    .build();
            when(repo.findByUserId(10L)).thenReturn(Optional.of(saved));

            UserPrivacySettingsDto dto = service.getSettings(10L);

            assertThat(dto.getShowPhoneToNeighbours()).isTrue();
            assertThat(dto.getActivityVisibility()).isEqualTo("PRIVATE");
        }

        @Test
        @DisplayName("returns safe defaults when no settings exist yet")
        void returnsDefaults_whenNoSettingsSaved() {
            when(repo.findByUserId(99L)).thenReturn(Optional.empty());

            UserPrivacySettingsDto dto = service.getSettings(99L);

            // Privacy-safe defaults
            assertThat(dto.getShowPhoneToNeighbours()).isFalse();
            assertThat(dto.getShowEmailToNeighbours()).isFalse();
            assertThat(dto.getShowFlatInDirectory()).isTrue(); // OK to show flat
            assertThat(dto.getShowFamilyMembers()).isFalse(); // private by default
            assertThat(dto.getShowVehicleInDirectory()).isFalse();
            assertThat(dto.getEmergencyContactRestricted()).isTrue();
            assertThat(dto.getActivityVisibility()).isEqualTo("COMMUNITY");
        }
    }

    @Nested
    @DisplayName("updateSettings")
    class UpdateSettings {

        @Test
        @DisplayName("persists updated preferences")
        void persistsUpdate() {
            UserPrivacySettings existing = UserPrivacySettings.builder()
                    .id(1L).userId(10L)
                    .showPhoneToNeighbours(false)
                    .showEmailToNeighbours(false)
                    .showFlatInDirectory(true)
                    .showFamilyMembers(false)
                    .allowMarketplaceContact(true)
                    .allowEventTagging(true)
                    .activityVisibility("COMMUNITY")
                    .build();
            when(repo.findByUserId(10L)).thenReturn(Optional.of(existing));
            when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            UserPrivacySettingsDto update = UserPrivacySettingsDto.builder()
                    .showPhoneToNeighbours(true)
                    .activityVisibility("PRIVATE")
                    .build();

            UserPrivacySettingsDto result = service.updateSettings(10L, update);

            assertThat(result.getShowPhoneToNeighbours()).isTrue();
            assertThat(result.getActivityVisibility()).isEqualTo("PRIVATE");
            // Unchanged fields remain
            assertThat(result.getShowEmailToNeighbours()).isFalse();
        }

        @Test
        @DisplayName("creates new settings record if none exists")
        void createsNewRecord_whenNoneExists() {
            when(repo.findByUserId(55L)).thenReturn(Optional.empty());
            when(repo.save(any())).thenAnswer(inv -> {
                UserPrivacySettings s = inv.getArgument(0);
                s.setId(10L);
                return s;
            });

            UserPrivacySettingsDto update = UserPrivacySettingsDto.builder()
                    .showPhoneToNeighbours(true)
                    .build();

            service.updateSettings(55L, update);

            ArgumentCaptor<UserPrivacySettings> captor = ArgumentCaptor.forClass(UserPrivacySettings.class);
            verify(repo).save(captor.capture());
            assertThat(captor.getValue().getUserId()).isEqualTo(55L);
        }

        @Test
        @DisplayName("audit is recorded on every update")
        void auditIsRecorded() {
            when(repo.findByUserId(10L)).thenReturn(Optional.empty());
            when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.updateSettings(10L, UserPrivacySettingsDto.builder().build());

            verify(privacyAuditService).record(anyString(), anyString(), anyString());
        }
    }
}

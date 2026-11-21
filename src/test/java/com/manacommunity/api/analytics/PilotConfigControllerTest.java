package com.manacommunity.api.analytics;

import com.manacommunity.api.analytics.controller.PilotConfigController;
import com.manacommunity.api.analytics.dto.PilotConfigRequest;
import com.manacommunity.api.analytics.dto.PilotConfigResponse;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.model.PilotCommunityConfig;
import com.manacommunity.api.repository.CommunityRepository;
import com.manacommunity.api.repository.PilotCommunityConfigRepository;
import com.manacommunity.api.service.CommunityModuleService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PilotConfigController")
class PilotConfigControllerTest {

    @Mock
    private PilotCommunityConfigRepository pilotConfigRepo;

    @Mock
    private CommunityRepository communityRepository;

    @Mock
    private CommunityModuleService communityModuleService;

    @InjectMocks
    private PilotConfigController controller;

    @Test
    @DisplayName("getPilotConfig returns pilot status and enabled modules")
    void getPilotConfigSuccess() {
        when(pilotConfigRepo.findById(10L)).thenReturn(Optional.of(
                PilotCommunityConfig.builder().communityId(10L).isPilot(true).cohort("cohort-A").build()
        ));
        when(communityModuleService.getEnabledModuleKeys(10L)).thenReturn(List.of("SPORTS", "COMMERCE"));

        ResponseEntity<PilotConfigResponse> response = controller.getPilotConfig(10L);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isPilot()).isTrue();
        assertThat(response.getBody().getCohort()).isEqualTo("cohort-A");
        assertThat(response.getBody().getEnabledModules()).containsExactly("SPORTS", "COMMERCE");
    }

    @Test
    @DisplayName("getPilotConfig defaults to non-pilot when no record exists")
    void getPilotConfigDefault() {
        when(pilotConfigRepo.findById(99L)).thenReturn(Optional.empty());
        when(communityModuleService.getEnabledModuleKeys(99L)).thenReturn(List.of());

        ResponseEntity<PilotConfigResponse> response = controller.getPilotConfig(99L);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isPilot()).isFalse();
        assertThat(response.getBody().getCohort()).isNull();
    }

    @Test
    @DisplayName("updatePilotConfig updates pilot and cohort")
    void updatePilotConfigSuccess() {
        Community community = Community.builder().id(10L).name("Test Community").build();
        when(communityRepository.findById(10L)).thenReturn(Optional.of(community));
        when(pilotConfigRepo.findById(10L)).thenReturn(Optional.empty());
        when(communityModuleService.getEnabledModuleKeys(10L)).thenReturn(List.of("AMENITY_BOOKING"));

        PilotConfigRequest req = PilotConfigRequest.builder().pilot(true).cohort("pilot-cohort-1").build();
        ResponseEntity<PilotConfigResponse> response = controller.updatePilotConfig(10L, req);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isPilot()).isTrue();
        assertThat(response.getBody().getCohort()).isEqualTo("pilot-cohort-1");
        verify(pilotConfigRepo).save(any(PilotCommunityConfig.class));
    }
}

package com.manacommunity.api.unit.service;

import com.manacommunity.api.dto.CommunityModuleBulkRequest;
import com.manacommunity.api.dto.CommunityModuleResponse;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.model.CommunityModule;
import com.manacommunity.api.repository.CommunityModuleRepository;
import com.manacommunity.api.repository.CommunityRepository;
import com.manacommunity.api.service.impl.CommunityModuleServiceImpl;
import com.manacommunity.api.support.TestDataBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommunityModuleService")
class CommunityModuleServiceTest {

    @Mock CommunityModuleRepository communityModuleRepository;
    @Mock CommunityRepository communityRepository;

    @InjectMocks CommunityModuleServiceImpl communityModuleService;

    @Test
    @DisplayName("initializeModulesForCommunity sets COMMUNITY_FEED and ADMIN_HUB to true, others to false, with explicit insertion timestamps")
    void initializeModulesForCommunityTest() {
        Community community = TestDataBuilder.community();
        when(communityModuleRepository.existsByCommunityId(1L)).thenReturn(false);
        when(communityRepository.findById(1L)).thenReturn(Optional.of(community));

        communityModuleService.initializeModulesForCommunity(1L);

        ArgumentCaptor<List<CommunityModule>> listCaptor = ArgumentCaptor.forClass(List.class);
        verify(communityModuleRepository).saveAll(listCaptor.capture());

        List<CommunityModule> savedModules = listCaptor.getValue();
        assertThat(savedModules).isNotEmpty();

        for (CommunityModule cm : savedModules) {
            assertThat(cm.getCommunity()).isEqualTo(community);
            assertThat(cm.getCreatedAt()).isNotNull();
            assertThat(cm.getUpdatedAt()).isNotNull();

            assertThat(cm.getIsEnabled()).isFalse();
        }
    }

    @Test
    @DisplayName("isModuleEnabled returns false by default if community modules do not exist")
    void isModuleEnabledDefaultFalse() {
        when(communityModuleRepository.existsByCommunityId(1L)).thenReturn(false);

        boolean enabled = communityModuleService.isModuleEnabled(1L, "SPORTS");

        assertThat(enabled).isFalse();
        verify(communityModuleRepository, never()).isModuleEnabled(anyLong(), anyString());
    }

    @Test
    @DisplayName("isModuleEnabled delegates to repository when community modules exist")
    void isModuleEnabledDelegates() {
        when(communityModuleRepository.existsByCommunityId(1L)).thenReturn(true);
        when(communityModuleRepository.isModuleEnabled(1L, "SPORTS")).thenReturn(true);

        boolean enabled = communityModuleService.isModuleEnabled(1L, "SPORTS");

        assertThat(enabled).isTrue();
    }

    @Test
    @DisplayName("bulkUpdate successfully processes known and new module keys without 404")
    void bulkUpdateWithNewModulesSucceeds() {
        Community community = TestDataBuilder.community();
        when(communityRepository.findById(2L)).thenReturn(Optional.of(community));
        when(communityModuleRepository.findByCommunityIdAndModuleKey(2L, "SPORTS")).thenReturn(Optional.empty());
        when(communityModuleRepository.findByCommunityIdAndModuleKey(2L, "ENTERTAINMENT")).thenReturn(Optional.empty());
        when(communityModuleRepository.findByCommunityIdAndModuleKey(2L, "CUSTOM_UNKNOWN_MODULE")).thenReturn(Optional.empty());
        when(communityModuleRepository.save(any(CommunityModule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CommunityModuleBulkRequest request = new CommunityModuleBulkRequest(
                2L,
                List.of(
                        new CommunityModuleBulkRequest.ModuleToggle("SPORTS", true),
                        new CommunityModuleBulkRequest.ModuleToggle("ENTERTAINMENT", true),
                        new CommunityModuleBulkRequest.ModuleToggle("CUSTOM_UNKNOWN_MODULE", false)
                )
        );

        List<CommunityModuleResponse> responses = communityModuleService.bulkUpdate(request);

        assertThat(responses).hasSize(3);
        assertThat(responses.get(0).moduleKey()).isEqualTo("SPORTS");
        assertThat(responses.get(0).isEnabled()).isTrue();
        assertThat(responses.get(1).moduleKey()).isEqualTo("ENTERTAINMENT");
        assertThat(responses.get(1).isEnabled()).isTrue();
        assertThat(responses.get(2).moduleKey()).isEqualTo("CUSTOM_UNKNOWN_MODULE");
        assertThat(responses.get(2).moduleLabel()).isEqualTo("Custom Unknown Module");
        assertThat(responses.get(2).isEnabled()).isFalse();

        verify(communityModuleRepository, times(3)).save(any(CommunityModule.class));
    }

    @Test
    @DisplayName("toggleModule successfully creates missing module and toggles enabled state")
    void toggleModuleCreatesIfMissing() {
        Community community = TestDataBuilder.community();
        when(communityRepository.findById(2L)).thenReturn(Optional.of(community));
        when(communityModuleRepository.findByCommunityIdAndModuleKey(2L, "ENTERTAINMENT")).thenReturn(Optional.empty());
        when(communityModuleRepository.save(any(CommunityModule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CommunityModuleResponse response = communityModuleService.toggleModule(2L, "ENTERTAINMENT", true);

        assertThat(response.moduleKey()).isEqualTo("ENTERTAINMENT");
        assertThat(response.isEnabled()).isTrue();
        assertThat(response.moduleLabel()).isEqualTo("Entertainment & Movies");
        verify(communityModuleRepository).save(any(CommunityModule.class));
    }
}

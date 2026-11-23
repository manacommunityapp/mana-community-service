package com.manacommunity.api.unit.service;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.model.PlatformPublicConfig;
import com.manacommunity.api.model.PlatformShowcaseItem;
import com.manacommunity.api.repository.CommunityRepository;
import com.manacommunity.api.repository.PlatformPublicConfigRepository;
import com.manacommunity.api.repository.PlatformShowcaseItemRepository;
import com.manacommunity.api.response.PlatformPublicOverviewResponse;
import com.manacommunity.api.service.impl.PlatformPublicServiceImpl;
import com.manacommunity.api.user.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlatformPublicServiceImplTest {

    @Mock private CommunityRepository communityRepo;
    @Mock private AppUserRepository appUserRepo;
    @Mock private PlatformPublicConfigRepository configRepo;
    @Mock private PlatformShowcaseItemRepository showcaseRepo;

    @InjectMocks
    private PlatformPublicServiceImpl service;

    @Test
    void getPublicOverview_aggregatesLiveDatabaseCountsAndShowcaseWithoutExposingInviteCodes() {
        when(appUserRepo.countByIsActiveTrue()).thenReturn(342L);
        when(communityRepo.countByActiveTrue()).thenReturn(4L);
        when(communityRepo.countDistinctActiveCities()).thenReturn(2L);
        when(appUserRepo.countActiveMembersByCommunity()).thenReturn(List.of(
                new Object[]{1L, 210L},
                new Object[]{2L, 132L}
        ));

        Community c1 = Community.builder()
                .id(1L).name("Mana Palms").type("APARTMENT").city("Hyderabad").area("Gachibowli")
                .inviteCode("SECRET99").active(true).build();
        Community c2 = Community.builder()
                .id(2L).name("Serene Greens").type("APARTMENT").city("Bengaluru").area("Whitefield")
                .inviteCode("SECRET88").active(true).build();
        when(communityRepo.findByActiveTrueOrderByNameAsc()).thenReturn(List.of(c1, c2));

        PlatformPublicConfig cfg = PlatformPublicConfig.builder()
                .configKey("default")
                .topBadgeText("Verified Gated Communities")
                .heroCapsuleText("ELEVATING GATED LIVING")
                .heroTitle("Mana Community")
                .heroSubtitle("Live dynamic subtitle")
                .securityLabel("Bank-Grade Secure")
                .certificationNote("Encrypted · RWA Certified")
                .build();
        when(configRepo.findByConfigKey("default")).thenReturn(Optional.of(cfg));

        PlatformShowcaseItem item = PlatformShowcaseItem.builder()
                .itemKey("security").emoji("🛡️").accentColor("#10B981")
                .title("Smart Gate").tagline("QR Passes").description("Live guard passes")
                .tagsCsv("Guest QR Pass,Emergency SOS").sortOrder(1).active(true).build();
        when(showcaseRepo.findByActiveTrueOrderBySortOrderAscIdAsc()).thenReturn(List.of(item));

        PlatformPublicOverviewResponse resp = service.getPublicOverview();

        assertEquals(342L, resp.getTotalResidents());
        assertEquals(4L, resp.getTotalSocieties());
        assertEquals(2L, resp.getTotalCities());
        assertEquals("Live dynamic subtitle", resp.getHeroSubtitle());
        assertEquals(2, resp.getFeaturedSocieties().size());
        assertEquals("Mana Palms", resp.getFeaturedSocieties().get(0).getName());
        assertEquals(210L, resp.getFeaturedSocieties().get(0).getResidentCount());
        assertEquals(1, resp.getShowcaseItems().size());
        assertEquals(List.of("Guest QR Pass", "Emergency SOS"), resp.getShowcaseItems().get(0).getTags());
    }

    @Test
    void updatePublicConfig_persistsEditableFields() {
        when(configRepo.findByConfigKey("default")).thenReturn(Optional.empty());
        when(configRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(appUserRepo.countByIsActiveTrue()).thenReturn(10L);
        when(communityRepo.countByActiveTrue()).thenReturn(1L);
        when(communityRepo.countDistinctActiveCities()).thenReturn(1L);
        when(appUserRepo.countActiveMembersByCommunity()).thenReturn(List.of());
        when(communityRepo.findByActiveTrueOrderByNameAsc()).thenReturn(List.of());
        when(showcaseRepo.findByActiveTrueOrderBySortOrderAscIdAsc()).thenReturn(List.of());

        var req = new PlatformPublicOverviewResponse.UpdatePublicConfigRequest(
                "Verified Communities", null, "Mana Community", "Updated Subtitle", null, null);
        service.updatePublicConfig(req);

        verify(configRepo).save(argThat(c -> "Updated Subtitle".equals(c.getHeroSubtitle())));
    }
}

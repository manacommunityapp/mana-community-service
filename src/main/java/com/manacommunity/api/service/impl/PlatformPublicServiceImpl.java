package com.manacommunity.api.service.impl;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.model.PlatformPublicConfig;
import com.manacommunity.api.model.PlatformShowcaseItem;
import com.manacommunity.api.repository.CommunityRepository;
import com.manacommunity.api.repository.PlatformPublicConfigRepository;
import com.manacommunity.api.repository.PlatformShowcaseItemRepository;
import com.manacommunity.api.response.PlatformPublicOverviewResponse;
import com.manacommunity.api.response.PlatformPublicOverviewResponse.PublicSocietySummary;
import com.manacommunity.api.response.PlatformPublicOverviewResponse.ShowcaseItemDto;
import com.manacommunity.api.response.PlatformPublicOverviewResponse.UpdatePublicConfigRequest;
import com.manacommunity.api.service.PlatformPublicService;
import com.manacommunity.api.user.repository.AppUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PlatformPublicServiceImpl implements PlatformPublicService {

    private static final String DEFAULT_KEY = "default";

    @Autowired
    private CommunityRepository communityRepo;

    @Autowired
    private AppUserRepository appUserRepo;

    @Autowired
    private PlatformPublicConfigRepository configRepo;

    @Autowired
    private PlatformShowcaseItemRepository showcaseRepo;

    @Override
    @Transactional(readOnly = true)
    public PlatformPublicOverviewResponse getPublicOverview() {
        long totalResidents = appUserRepo.countByIsActiveTrue();
        long totalSocieties = communityRepo.countByActiveTrue();
        long totalCities = communityRepo.countDistinctActiveCities();

        Map<Long, Long> memberCounts = new HashMap<>();
        for (Object[] row : appUserRepo.countActiveMembersByCommunity()) {
            if (row[0] instanceof Number cid && row[1] instanceof Number cnt) {
                memberCounts.put(cid.longValue(), cnt.longValue());
            }
        }

        List<Community> activeCommunities = communityRepo.findByActiveTrueOrderByNameAsc();
        List<PublicSocietySummary> featuredSocieties = activeCommunities.stream()
                .map(c -> PublicSocietySummary.builder()
                        .id(c.getId())
                        .name(c.getName())
                        .type(c.getType())
                        .city(c.getCity())
                        .area(c.getArea())
                        .residentCount(memberCounts.getOrDefault(c.getId(), 0L))
                        .build())
                .sorted(Comparator.comparingLong(PublicSocietySummary::getResidentCount).reversed()
                        .thenComparing(PublicSocietySummary::getName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .limit(6)
                .collect(Collectors.toList());

        PlatformPublicConfig cfg = configRepo.findByConfigKey(DEFAULT_KEY)
                .orElseGet(PlatformPublicServiceImpl::defaultConfigFallback);

        List<PlatformShowcaseItem> dbItems = showcaseRepo.findByActiveTrueOrderBySortOrderAscIdAsc();
        List<ShowcaseItemDto> showcaseItems = dbItems.stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        return PlatformPublicOverviewResponse.builder()
                .totalResidents(totalResidents)
                .totalSocieties(totalSocieties)
                .totalCities(totalCities)
                .topBadgeText(cfg.getTopBadgeText())
                .heroCapsuleText(cfg.getHeroCapsuleText())
                .heroTitle(cfg.getHeroTitle())
                .heroSubtitle(cfg.getHeroSubtitle())
                .securityLabel(cfg.getSecurityLabel())
                .certificationNote(cfg.getCertificationNote())
                .featuredSocieties(featuredSocieties)
                .showcaseItems(showcaseItems)
                .build();
    }

    @Override
    @Transactional
    public PlatformPublicOverviewResponse updatePublicConfig(UpdatePublicConfigRequest request) {
        PlatformPublicConfig cfg = configRepo.findByConfigKey(DEFAULT_KEY)
                .orElseGet(() -> {
                    PlatformPublicConfig fallback = defaultConfigFallback();
                    fallback.setConfigKey(DEFAULT_KEY);
                    return fallback;
                });

        if (hasText(request.getTopBadgeText())) cfg.setTopBadgeText(request.getTopBadgeText().trim());
        if (hasText(request.getHeroCapsuleText())) cfg.setHeroCapsuleText(request.getHeroCapsuleText().trim());
        if (hasText(request.getHeroTitle())) cfg.setHeroTitle(request.getHeroTitle().trim());
        if (hasText(request.getHeroSubtitle())) cfg.setHeroSubtitle(request.getHeroSubtitle().trim());
        if (hasText(request.getSecurityLabel())) cfg.setSecurityLabel(request.getSecurityLabel().trim());
        if (hasText(request.getCertificationNote())) cfg.setCertificationNote(request.getCertificationNote().trim());

        configRepo.save(cfg);
        return getPublicOverview();
    }

    @Override
    @Transactional
    public List<ShowcaseItemDto> updateShowcaseItems(List<ShowcaseItemDto> items) {
        if (items == null) return List.of();
        for (int i = 0; i < items.size(); i++) {
            ShowcaseItemDto dto = items.get(i);
            if (!hasText(dto.getItemKey()) || !hasText(dto.getTitle())) continue;
            String key = dto.getItemKey().trim().toLowerCase();
            PlatformShowcaseItem entity = showcaseRepo.findByItemKey(key)
                    .orElseGet(() -> PlatformShowcaseItem.builder().itemKey(key).build());
            entity.setEmoji(hasText(dto.getEmoji()) ? dto.getEmoji().trim() : "✨");
            entity.setAccentColor(hasText(dto.getAccentColor()) ? dto.getAccentColor().trim() : "#4F46E5");
            entity.setTitle(dto.getTitle().trim());
            entity.setTagline(hasText(dto.getTagline()) ? dto.getTagline().trim() : "");
            entity.setDescription(hasText(dto.getDescription()) ? dto.getDescription().trim() : "");
            entity.setTagsCsv(dto.getTags() != null
                    ? dto.getTags().stream().filter(Objects::nonNull).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.joining(","))
                    : "");
            entity.setSortOrder(dto.getSortOrder() > 0 ? dto.getSortOrder() : i + 1);
            entity.setActive(true);
            showcaseRepo.save(entity);
        }
        return showcaseRepo.findByActiveTrueOrderBySortOrderAscIdAsc().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private ShowcaseItemDto toDto(PlatformShowcaseItem item) {
        List<String> tags = item.getTagsCsv() == null || item.getTagsCsv().isBlank()
                ? List.of()
                : Arrays.stream(item.getTagsCsv().split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toList());
        return ShowcaseItemDto.builder()
                .itemKey(item.getItemKey())
                .emoji(item.getEmoji())
                .accentColor(item.getAccentColor())
                .title(item.getTitle())
                .tagline(item.getTagline())
                .description(item.getDescription())
                .tags(tags)
                .sortOrder(item.getSortOrder() != null ? item.getSortOrder() : 0)
                .build();
    }

    private static PlatformPublicConfig defaultConfigFallback() {
        return PlatformPublicConfig.builder()
                .configKey(DEFAULT_KEY)
                .topBadgeText("Verified Gated Communities")
                .heroCapsuleText("ELEVATING GATED LIVING")
                .heroTitle("Mana Community")
                .heroSubtitle("Your home, neighbors, and society operations — seamlessly connected in your pocket.")
                .securityLabel("Bank-Grade Secure")
                .certificationNote("Encrypted · RWA & Apartment Association Certified")
                .build();
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }
}

package com.manacommunity.api.adtech.service;

import com.manacommunity.api.adtech.dto.AdTechDtos.*;
import com.manacommunity.api.adtech.entity.AdCampaignEntity;
import com.manacommunity.api.adtech.entity.AdClickEntity;
import com.manacommunity.api.adtech.entity.AdCreativeEntity;
import com.manacommunity.api.adtech.entity.AdImpressionEntity;
import com.manacommunity.api.adtech.repository.AdCampaignRepository;
import com.manacommunity.api.adtech.repository.AdClickRepository;
import com.manacommunity.api.adtech.repository.AdCreativeRepository;
import com.manacommunity.api.adtech.repository.AdImpressionRepository;
import com.manacommunity.api.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class AdServingService {

    private final AdCampaignRepository campaignRepository;
    private final AdCreativeRepository creativeRepository;
    private final AdImpressionRepository impressionRepository;
    private final AdClickRepository clickRepository;
    private final AdPacingRedisService pacingRedisService;

    @Transactional(readOnly = true)
    public Optional<AdResponseDto> selectAd(AdRequestDto req) {
        Instant now = Instant.now();
        List<AdCampaignEntity> activeCampaigns = campaignRepository
                .findByStatusAndStartAtLessThanEqualAndEndAtGreaterThanEqualAndDeletedFalse("ACTIVE", now, now);

        for (AdCampaignEntity campaign : activeCampaigns) {
            if (campaign.getSpentPaise() >= campaign.getTotalBudgetPaise()) {
                pacingRedisService.haltCampaign(campaign.getCampaignId().toString());
                continue;
            }
            if (!matchesTargeting(campaign.getTargeting(), req)) {
                continue;
            }

            List<AdCreativeEntity> creatives = creativeRepository.findByCampaignIdAndDeletedFalse(campaign.getCampaignId());
            if (creatives.isEmpty()) {
                continue;
            }

            String cid = campaign.getCampaignId().toString();
            int remaining = pacingRedisService.tryAcquireFrequencyCap(cid, req.userId(), campaign.getFreqCapPerDay());
            if (remaining < 0) {
                continue;
            }

            boolean tokenOk = pacingRedisService.tryConsumeBudgetToken(
                    cid,
                    campaign.getTotalBudgetPaise(),
                    campaign.getCpmPaise(),
                    campaign.getStartAt(),
                    campaign.getEndAt()
            );
            if (!tokenOk) {
                continue;
            }

            AdCreativeEntity selected = pickWeightedCreative(creatives);
            UUID impressionId = UUID.randomUUID();
            return Optional.of(new AdResponseDto(
                    impressionId.toString(),
                    cid,
                    toCreativeDto(selected, campaign.getBrandName()),
                    "/api/adtech/impression",
                    "/api/adtech/click",
                    remaining,
                    0L
            ));
        }
        return Optional.empty();
    }

    @Transactional
    public void recordImpression(ImpressionEventDto event) {
        if (event.impressionId() == null || event.campaignId() == null) {
            return;
        }
        if (!pacingRedisService.markImpressionIfNew(event.impressionId())) {
            return;
        }

        UUID impressionUuid = UUID.fromString(event.impressionId());
        if (impressionRepository.existsById(impressionUuid)) {
            return;
        }

        UUID campaignUuid = UUID.fromString(event.campaignId());
        AdCampaignEntity campaign = campaignRepository.findByCampaignIdAndDeletedFalse(campaignUuid).orElse(null);
        long costPaise = campaign != null ? Math.max(1L, campaign.getCpmPaise() / 1000L) : 0L;

        AdImpressionEntity entity = new AdImpressionEntity();
        entity.setImpressionId(impressionUuid);
        entity.setCampaignId(campaignUuid);
        entity.setUserId(event.userId() != null ? event.userId() : "unknown");
        entity.setSocietyId(event.societyId() != null ? event.societyId() : "unknown");
        entity.setPlacementId(event.placementId() != null ? event.placementId() : "default");
        entity.setCostPaise(costPaise);
        entity.setFiredAt(parseInstantOrNow(event.firedAt()));
        impressionRepository.save(entity);

        if (campaign != null && costPaise > 0) {
            campaignRepository.recordSpendAndCheckExhausted(campaignUuid, costPaise);
            if (campaign.getSpentPaise() + costPaise >= campaign.getTotalBudgetPaise()) {
                pacingRedisService.haltCampaign(campaignUuid.toString());
            }
        }
    }

    @Transactional
    public void recordClick(ClickEventDto event) {
        if (event.impressionId() == null || event.campaignId() == null) {
            return;
        }
        AdClickEntity click = new AdClickEntity();
        click.setImpressionId(UUID.fromString(event.impressionId()));
        click.setCampaignId(UUID.fromString(event.campaignId()));
        click.setClickedAt(Instant.now());
        clickRepository.save(click);
    }

    @Transactional(readOnly = true)
    public List<AdCampaignDto> listCampaigns(String status, String city) {
        return campaignRepository.findByDeletedFalseOrderByCreatedAtDesc().stream()
                .filter(c -> status == null || status.isBlank() || status.equalsIgnoreCase(c.getStatus()))
                .filter(c -> city == null || city.isBlank() || (c.getTargeting() != null
                        && c.getTargeting().cities() != null
                        && c.getTargeting().cities().stream().anyMatch(city::equalsIgnoreCase)))
                .map(this::toCampaignDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public AdCampaignDto getCampaign(String campaignId) {
        AdCampaignEntity entity = findCampaign(campaignId);
        return toCampaignDto(entity);
    }

    @Transactional(readOnly = true)
    public BudgetPacingStateDto getBudgetState(String campaignId) {
        AdCampaignEntity entity = findCampaign(campaignId);
        long remaining = Math.max(0L, entity.getTotalBudgetPaise() - entity.getSpentPaise());
        double pct = entity.getTotalBudgetPaise() > 0
                ? (entity.getSpentPaise() * 100.0) / entity.getTotalBudgetPaise()
                : 0.0;
        boolean halted = "PAUSED".equals(entity.getStatus())
                || "EXHAUSTED".equals(entity.getStatus())
                || pacingRedisService.isCampaignHalted(campaignId);

        return new BudgetPacingStateDto(
                entity.getCampaignId().toString(),
                entity.getTotalBudgetPaise(),
                entity.getSpentPaise(),
                remaining,
                pct,
                halted,
                Instant.now().toString()
        );
    }

    @Transactional
    public void pauseCampaign(String campaignId) {
        AdCampaignEntity entity = findCampaign(campaignId);
        entity.setStatus("PAUSED");
        campaignRepository.save(entity);
        pacingRedisService.haltCampaign(campaignId);
    }

    @Transactional
    public void resumeCampaign(String campaignId) {
        AdCampaignEntity entity = findCampaign(campaignId);
        if (entity.getSpentPaise() < entity.getTotalBudgetPaise()) {
            entity.setStatus("ACTIVE");
            campaignRepository.save(entity);
            pacingRedisService.unhaltCampaign(campaignId);
        }
    }

    private AdCampaignEntity findCampaign(String campaignId) {
        return campaignRepository.findByCampaignIdAndDeletedFalse(UUID.fromString(campaignId))
                .orElseThrow(() -> new ResourceNotFoundException("AdCampaign", "campaignId", campaignId));
    }

    private boolean matchesTargeting(CampaignTargetingDto targeting, AdRequestDto req) {
        if (targeting == null) {
            return true;
        }
        if (targeting.cities() != null && !targeting.cities().isEmpty()) {
            if (req.city() == null || targeting.cities().stream().noneMatch(c -> c.equalsIgnoreCase(req.city()))) {
                return false;
            }
        }
        if (targeting.flatTypes() != null && !targeting.flatTypes().isEmpty()) {
            if (req.flatType() == null || targeting.flatTypes().stream().noneMatch(f -> f.equalsIgnoreCase(req.flatType()))) {
                return false;
            }
        }
        if (targeting.societyIds() != null && !targeting.societyIds().isEmpty()) {
            if (req.societyId() == null || targeting.societyIds().stream().noneMatch(s -> s.equalsIgnoreCase(req.societyId()))) {
                return false;
            }
        }
        return true;
    }

    private AdCreativeEntity pickWeightedCreative(List<AdCreativeEntity> creatives) {
        int totalWeight = creatives.stream().mapToInt(c -> Math.max(1, c.getWeight())).sum();
        int roll = ThreadLocalRandom.current().nextInt(totalWeight);
        int cumulative = 0;
        for (AdCreativeEntity c : creatives) {
            cumulative += Math.max(1, c.getWeight());
            if (roll < cumulative) {
                return c;
            }
        }
        return creatives.get(0);
    }

    private AdCampaignDto toCampaignDto(AdCampaignEntity entity) {
        List<AdCreativeDto> creatives = creativeRepository.findByCampaignIdAndDeletedFalse(entity.getCampaignId())
                .stream()
                .map(c -> toCreativeDto(c, entity.getBrandName()))
                .toList();
        return new AdCampaignDto(
                entity.getCampaignId().toString(),
                entity.getBrandId(),
                entity.getBrandName(),
                entity.getName(),
                entity.getTotalBudgetPaise(),
                entity.getSpentPaise(),
                entity.getCpmPaise(),
                entity.getStartAt().toString(),
                entity.getEndAt().toString(),
                entity.getStatus(),
                entity.getDailyCapPaise(),
                entity.getFreqCapPerDay(),
                entity.getTargeting(),
                creatives
        );
    }

    private AdCreativeDto toCreativeDto(AdCreativeEntity c, String brandName) {
        return new AdCreativeDto(
                c.getCreativeId().toString(),
                c.getCampaignId().toString(),
                c.getCreativeType(),
                c.getAssetUrl(),
                c.getCtaUrl(),
                c.getCtaText(),
                c.getWeight(),
                c.getWidthPx(),
                c.getHeightPx(),
                brandName,
                c.getBrandLogoUrl(),
                c.getHeadline(),
                c.getBodyText()
        );
    }

    private Instant parseInstantOrNow(String iso) {
        if (iso == null || iso.isBlank()) {
            return Instant.now();
        }
        try {
            return Instant.parse(iso);
        } catch (Exception ignored) {
            return Instant.now();
        }
    }
}

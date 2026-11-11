package com.manacommunity.api.mw.service;

import com.manacommunity.api.mw.dto.UpdateIntegrationConfigRequest;
import com.manacommunity.api.mw.entity.MwIntegrationConfig;
import com.manacommunity.api.mw.repository.MwIntegrationConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class MwIntegrationConfigService {

    private final MwIntegrationConfigRepository configRepo;

    public MwIntegrationConfig getOrCreateConfig(Long orgId) {
        return configRepo.findByOrganizationId(orgId)
                .orElseGet(() -> configRepo.save(MwIntegrationConfig.builder()
                        .organizationId(orgId)
                        .freshEnabled(false)
                        .homesEnabled(false)
                        .sportsEnabled(false)
                        .baseDeliveryPay(BigDecimal.ZERO)
                        .perKmRate(BigDecimal.ZERO)
                        .onTimeBonus(BigDecimal.ZERO)
                        .build()));
    }

    public MwIntegrationConfig updateConfig(Long orgId, UpdateIntegrationConfigRequest req) {
        MwIntegrationConfig config = getOrCreateConfig(orgId);
        if (req.getFreshEnabled() != null) config.setFreshEnabled(req.getFreshEnabled());
        if (req.getHomesEnabled() != null) config.setHomesEnabled(req.getHomesEnabled());
        if (req.getSportsEnabled() != null) config.setSportsEnabled(req.getSportsEnabled());
        if (req.getBaseDeliveryPay() != null) config.setBaseDeliveryPay(req.getBaseDeliveryPay());
        if (req.getPerKmRate() != null) config.setPerKmRate(req.getPerKmRate());
        if (req.getOnTimeBonus() != null) config.setOnTimeBonus(req.getOnTimeBonus());
        return configRepo.save(config);
    }
}

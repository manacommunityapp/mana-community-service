package com.manacommunity.api.platform.tenant.provisioning;

import com.manacommunity.api.config.tenant.TenantContext;
import com.manacommunity.api.dto.BlockConfigResponse;
import com.manacommunity.api.dto.CommunityModuleBulkRequest;
import com.manacommunity.api.dto.CommunityModuleResponse;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.model.TenantResourceQuota;
import com.manacommunity.api.platform.tenant.TenantConnectionQuotaManager;
import com.manacommunity.api.platform.tenant.TenantQueryTimeoutService;
import com.manacommunity.api.platform.tenant.TenantRateLimitService;
import com.manacommunity.api.repository.CommunityRepository;
import com.manacommunity.api.repository.RoleRepository;
import com.manacommunity.api.repository.TenantResourceQuotaRepository;
import com.manacommunity.api.service.CommunityBlockConfigService;
import com.manacommunity.api.service.CommunityModuleService;
import com.manacommunity.api.service.impl.CommunityRoleInitializer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;

/**
 * Single-call multi-tenant onboarding engine.
 *
 * <p>Provisions a new community in a shared-application, shared-database topology by atomically
 * creating the community record, seeding all baseline RBAC roles & permissions, initializing
 * feature modules, configuring blocks/towers, persisting DB resource quotas, and activating
 * runtime noisy-neighbor guardrails (rate limits, connection bulkheads, and query timeouts).</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommunityProvisioningService {

    private static final String INVITE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final List<String> DEFAULT_QUOTA_RESOURCES = List.of(
            "GENERAL", "VOTING_AGM", "BILLING_REPORT", "EXPORT"
    );

    private final CommunityRepository communityRepository;
    private final CommunityRoleInitializer communityRoleInitializer;
    private final CommunityModuleService communityModuleService;
    private final CommunityBlockConfigService blockConfigService;
    private final RoleRepository roleRepository;
    private final TenantResourceQuotaRepository quotaRepository;
    private final TenantRateLimitService rateLimitService;
    private final TenantConnectionQuotaManager connectionQuotaManager;
    private final TenantQueryTimeoutService queryTimeoutService;

    @Transactional
    public CommunityProvisionResponse provisionCommunity(CommunityProvisionRequest request) {
        TenantTier tier = TenantTier.fromStringOrDefault(request.getTenantTier());
        String normalizedType = (request.getType() != null && !request.getType().isBlank())
                ? request.getType().trim().toUpperCase()
                : "APARTMENT";
        Long orgId = request.getOrganizationId() != null
                ? request.getOrganizationId()
                : TenantContext.DEFAULT_ORG_ID;

        String inviteCode = (request.getInviteCode() != null && !request.getInviteCode().isBlank())
                ? request.getInviteCode().trim().toUpperCase()
                : null;

        // Idempotency guard: if inviteCode already exists, ensure baseline setup and return manifest
        if (inviteCode != null) {
            Optional<Community> existingOpt = communityRepository.findByInviteCode(inviteCode);
            if (existingOpt.isPresent()) {
                Community existing = existingOpt.get();
                log.info("[CommunityProvision] Idempotent hit for inviteCode={} (communityId={})",
                        inviteCode, existing.getId());
                return finalizeProvisioning(existing, request, tier, "ALREADY_EXISTS");
            }
        } else {
            inviteCode = generateUniqueInviteCode(request.getName());
        }

        Community community = Community.builder()
                .organizationId(orgId)
                .name(request.getName().trim())
                .type(normalizedType)
                .city(request.getCity())
                .state(request.getState())
                .area(request.getArea())
                .subtype(request.getSubtype())
                .inviteCode(inviteCode)
                .active(true)
                .build();

        Community saved = communityRepository.save(community);
        log.info("[CommunityProvision] Provisioned community '{}' (id={}, inviteCode={}, tier={})",
                saved.getName(), saved.getId(), saved.getInviteCode(), tier);

        return finalizeProvisioning(saved, request, tier, "PROVISIONED");
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getIsolationStatus(Long communityId) {
        Community community = communityRepository.findById(communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Community", communityId));

        List<TenantResourceQuota> quotas = quotaRepository.findByCommunityIdAndActiveTrue(communityId);
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("communityId", community.getId());
        status.put("communityName", community.getName());
        status.put("active", community.getActive());
        status.put("activeConnectionLeases", connectionQuotaManager.getActiveLeases(communityId));
        status.put("availableConnectionLeases", connectionQuotaManager.getAvailableLeases(communityId));
        status.put("maxConnectionLeases", connectionQuotaManager.getMaxAllowed(communityId));
        status.put("statementTimeoutMs", queryTimeoutService.getStatementTimeoutMs(communityId));
        status.put("dbResourceQuotas", quotas.stream().map(q -> Map.<String, Object>of(
                "resourceType", q.getResourceType(),
                "rateLimitPerMinute", q.getRateLimitPerMinute(),
                "maxConcurrentQueries", q.getMaxConcurrentQueries(),
                "maxRowsPerExport", q.getMaxRowsPerExport(),
                "priorityWeight", q.getPriorityWeight()
        )).toList());
        return status;
    }

    private CommunityProvisionResponse finalizeProvisioning(
            Community community,
            CommunityProvisionRequest request,
            TenantTier tier,
            String status
    ) {
        Long communityId = community.getId();

        // 1. Seed baseline roles & RBAC permissions
        communityRoleInitializer.initializeCommunityRoles(community);
        int rolesCount = roleRepository.findByCommunityId(communityId).size();

        // 2. Seed and selectively configure feature modules (via bulkUpdate so Redis & local cache stay consistent)
        communityModuleService.initializeModulesForCommunity(communityId);
        if (request.getEnabledModules() != null && !request.getEnabledModules().isEmpty()) {
            Set<String> requestedKeys = new HashSet<>(request.getEnabledModules());
            List<CommunityModuleResponse> existingModules = communityModuleService.getModulesForCommunity(communityId);
            List<CommunityModuleBulkRequest.ModuleToggle> toggles = new ArrayList<>();
            Set<String> seenKeys = new HashSet<>();
            for (CommunityModuleResponse m : existingModules) {
                seenKeys.add(m.moduleKey());
                toggles.add(new CommunityModuleBulkRequest.ModuleToggle(
                        m.moduleKey(), requestedKeys.contains(m.moduleKey())));
            }
            for (String key : requestedKeys) {
                if (!seenKeys.contains(key)) {
                    toggles.add(new CommunityModuleBulkRequest.ModuleToggle(key, true));
                }
            }
            if (!toggles.isEmpty()) {
                communityModuleService.bulkUpdate(new CommunityModuleBulkRequest(communityId, toggles));
            }
        }
        List<String> enabledModules = communityModuleService.getEnabledModuleKeys(communityId);

        // 3. Seed block/tower hierarchy
        if (request.getBlockConfigs() != null && !request.getBlockConfigs().isEmpty()) {
            blockConfigService.saveAllBlockConfigs(communityId, request.getBlockConfigs());
        } else if ("APARTMENT".equalsIgnoreCase(community.getType())) {
            blockConfigService.seedDefaultBlocks(communityId);
        }
        List<BlockConfigResponse> blocks = blockConfigService.getBlockConfigs(communityId);

        // 4. Seed noisy-neighbor DB resource quotas + runtime isolation limits
        int quotasSeeded = seedResourceQuotas(community, tier);
        rateLimitService.setTenantLimit(communityId, tier.getRateLimitPerMinute());
        connectionQuotaManager.setTenantQuota(communityId, tier.getConnectionPoolQuota());
        queryTimeoutService.setTenantStatementTimeout(communityId, tier.getStatementTimeoutMs());

        return CommunityProvisionResponse.builder()
                .communityId(communityId)
                .organizationId(community.getOrganizationId())
                .name(community.getName())
                .type(community.getType())
                .city(community.getCity())
                .state(community.getState())
                .area(community.getArea())
                .inviteCode(community.getInviteCode())
                .tenantTier(tier)
                .status(status)
                .rolesSeededCount(rolesCount)
                .enabledModules(enabledModules)
                .blocksConfiguredCount(blocks != null ? blocks.size() : 0)
                .quotasSeededCount(quotasSeeded)
                .rateLimitPerMinute(tier.getRateLimitPerMinute())
                .connectionPoolQuota(tier.getConnectionPoolQuota())
                .statementTimeoutMs(tier.getStatementTimeoutMs())
                .provisionedAt(Instant.now())
                .build();
    }

    private int seedResourceQuotas(Community community, TenantTier tier) {
        int seeded = 0;
        for (String resourceType : DEFAULT_QUOTA_RESOURCES) {
            if (quotaRepository.findByCommunityIdAndResourceType(community.getId(), resourceType).isEmpty()) {
                int maxConcurrent = "VOTING_AGM".equals(resourceType) || "BILLING_REPORT".equals(resourceType)
                        ? Math.max(2, tier.getMaxConcurrentQueries() / 2)
                        : tier.getMaxConcurrentQueries();
                TenantResourceQuota quota = TenantResourceQuota.builder()
                        .community(community)
                        .resourceType(resourceType)
                        .maxConcurrentQueries(maxConcurrent)
                        .maxRowsPerExport(tier.getMaxRowsPerExport())
                        .rateLimitPerMinute(tier.getRateLimitPerMinute())
                        .priorityWeight(tier == TenantTier.MEGA_TOWNSHIP ? 20 : 10)
                        .burstTokens(Math.max(20, tier.getRateLimitPerMinute() / 10))
                        .active(true)
                        .build();
                quotaRepository.save(quota);
                seeded++;
            }
        }
        return seeded;
    }

    private String generateUniqueInviteCode(String communityName) {
        String prefix = communityName.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        if (prefix.length() > 4) {
            prefix = prefix.substring(0, 4);
        } else if (prefix.isEmpty()) {
            prefix = "MANA";
        }
        for (int attempt = 0; attempt < 10; attempt++) {
            StringBuilder sb = new StringBuilder(prefix).append('-');
            for (int i = 0; i < 4; i++) {
                sb.append(INVITE_CHARS.charAt(RANDOM.nextInt(INVITE_CHARS.length())));
            }
            String candidate = sb.toString();
            if (communityRepository.findByInviteCode(candidate).isEmpty()) {
                return candidate;
            }
        }
        return "MANA-" + Long.toString(Math.abs(RANDOM.nextLong()), 36).substring(0, 6).toUpperCase();
    }
}

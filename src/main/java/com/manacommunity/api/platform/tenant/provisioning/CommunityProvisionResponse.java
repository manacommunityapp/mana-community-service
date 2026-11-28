package com.manacommunity.api.platform.tenant.provisioning;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Provisioning manifest returned by {@code POST /api/community/provision}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunityProvisionResponse {

    private Long communityId;
    private Long organizationId;
    private String name;
    private String type;
    private String city;
    private String state;
    private String area;
    private String inviteCode;
    private TenantTier tenantTier;
    /** PROVISIONED or ALREADY_EXISTS */
    private String status;
    private int rolesSeededCount;
    private List<String> enabledModules;
    private int blocksConfiguredCount;
    private int quotasSeededCount;
    private int rateLimitPerMinute;
    private int connectionPoolQuota;
    private long statementTimeoutMs;
    private Instant provisionedAt;
}

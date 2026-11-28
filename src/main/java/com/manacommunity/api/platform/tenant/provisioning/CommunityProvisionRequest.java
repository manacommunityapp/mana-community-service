package com.manacommunity.api.platform.tenant.provisioning;

import com.manacommunity.api.dto.BlockConfigResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * One-call request payload to provision a complete multi-tenant community
 * (entity, baseline roles & permissions, modules, blocks/towers, and isolation quotas).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunityProvisionRequest {

    @NotBlank(message = "Community name is required")
    @Size(max = 100, message = "Community name must be <= 100 characters")
    private String name;

    /** APARTMENT, COLLEGE, SCHOOL, OFFICE */
    @Builder.Default
    private String type = "APARTMENT";

    @Size(max = 60)
    private String city;

    @Size(max = 40)
    private String state;

    @Size(max = 100)
    private String area;

    @Size(max = 40)
    private String subtype;

    private Long organizationId;

    /** Optional custom invite code; auto-generated if null/blank. Also acts as idempotency key. */
    @Size(max = 20)
    private String inviteCode;

    /** STANDARD, LARGE, MEGA_TOWNSHIP */
    private String tenantTier;

    /** Optional subset of module keys to enable; null keeps platform defaults. */
    private List<String> enabledModules;

    /** Optional block/tower layout; null seeds A/B/C/D defaults for APARTMENTs. */
    private List<BlockConfigResponse> blockConfigs;
}

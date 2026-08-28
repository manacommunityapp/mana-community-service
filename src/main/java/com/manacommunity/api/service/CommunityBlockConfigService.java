package com.manacommunity.api.service;

import com.manacommunity.api.dto.BlockConfigRequest;
import com.manacommunity.api.dto.BlockConfigResponse;

import java.util.List;

public interface CommunityBlockConfigService {

    /**
     * Returns the full block -> floor -> flat hierarchy for a community.
     * Flat number lists are computed in-memory; nothing extra is persisted.
     */
    List<BlockConfigResponse> getBlockConfigs(Long communityId);

    /**
     * Idempotent: seeds the default A/B/C/D blocks for an APARTMENT community.
     * Does nothing if blocks are already configured.
     */
    void seedDefaultBlocks(Long communityId);

    /**
     * Admin: create or update a single block config.
     */
    BlockConfigResponse saveBlockConfig(Long communityId, BlockConfigRequest request);

    /**
     * Validates that the given block name and flat number are valid
     * for the supplied community.
     * Returns true if community has no block config (non-APARTMENT).
     * Throws IllegalArgumentException if the block/flat is out of range.
     */
    boolean validateBlockAndFlat(Long communityId, String blockName, String flatNo);
}

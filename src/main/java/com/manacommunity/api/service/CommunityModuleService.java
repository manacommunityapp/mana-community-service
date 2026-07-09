package com.manacommunity.api.service;

import com.manacommunity.api.dto.CommunityModuleBulkRequest;
import com.manacommunity.api.dto.CommunityModuleResponse;

import java.util.List;

public interface CommunityModuleService {

    List<CommunityModuleResponse> getModulesForCommunity(Long communityId);

    List<String> getEnabledModuleKeys(Long communityId);

    CommunityModuleResponse toggleModule(Long communityId, String moduleKey, boolean enabled);

    List<CommunityModuleResponse> bulkUpdate(CommunityModuleBulkRequest request);

    void initializeModulesForCommunity(Long communityId);

    boolean isModuleEnabled(Long communityId, String moduleKey);
}

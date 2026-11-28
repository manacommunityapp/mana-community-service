package com.manacommunity.api.service;

import com.manacommunity.api.response.PlatformPublicOverviewResponse;
import com.manacommunity.api.response.PlatformPublicOverviewResponse.ShowcaseItemDto;
import com.manacommunity.api.response.PlatformPublicOverviewResponse.UpdatePublicConfigRequest;

import java.util.List;

public interface PlatformPublicService {
    PlatformPublicOverviewResponse getPublicOverview();

    PlatformPublicOverviewResponse updatePublicConfig(UpdatePublicConfigRequest request);

    List<ShowcaseItemDto> updateShowcaseItems(List<ShowcaseItemDto> items);
}

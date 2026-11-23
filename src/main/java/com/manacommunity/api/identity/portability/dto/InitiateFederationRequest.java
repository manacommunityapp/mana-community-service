package com.manacommunity.api.identity.portability.dto;

import lombok.Data;

@Data
public class InitiateFederationRequest {
    private Long targetCommunityId;
    private String linkType;
}

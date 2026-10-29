package com.manacommunity.api.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProposalSupportResponse {
    private boolean success;
    private int supportCount;
    private boolean hasUserSupported;
}

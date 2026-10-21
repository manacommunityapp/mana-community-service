package com.manacommunity.api.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class CposCrmInboundConfigDto {
    private Long id;
    private String apiKey;
    private List<String> agentPool;
    private int rrIndex;
    private long totalInboundLeads;
}

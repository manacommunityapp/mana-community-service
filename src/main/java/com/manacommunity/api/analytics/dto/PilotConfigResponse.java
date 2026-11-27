package com.manacommunity.api.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PilotConfigResponse {
    private boolean pilot;
    private String cohort;
    private List<String> enabledModules;
}

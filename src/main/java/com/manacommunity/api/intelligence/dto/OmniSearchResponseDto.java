package com.manacommunity.api.intelligence.dto;

import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OmniSearchResponseDto {
    private String query;
    private int totalResults;
    private Map<String, Integer> domainCounts;
    private List<OmniSearchItemDto> people;
    private List<OmniSearchItemDto> events;
    private List<OmniSearchItemDto> sports;
    private List<OmniSearchItemDto> jobs;
    private List<OmniSearchItemDto> businesses;
    private List<OmniSearchItemDto> services;
    private List<OmniSearchItemDto> marketplace;
    private List<OmniSearchItemDto> food;
    private List<OmniSearchItemDto> documents;
    private List<OmniSearchItemDto> vendors;
}
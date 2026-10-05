package com.manacommunity.api.intelligence.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OmniSearchItemDto {
    private String id;
    private String domain;
    private String title;
    private String subtitle;
    private String description;
    private String badge;
    private String avatarUrl;
    private String deepLink;
    private double relevanceScore;
    private List<String> tags;
}
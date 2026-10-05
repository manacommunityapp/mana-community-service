package com.manacommunity.api.intelligence.dto;

import com.manacommunity.api.intelligence.model.RecommendationType;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendationCardDto {
    private String id;
    private RecommendationType type;
    private String title;
    private String subtitle;
    private String description;
    private int score;
    private List<String> tags;
    private String actionLabel;
    private String actionPath;
    private String imagePlaceholderColor;
}
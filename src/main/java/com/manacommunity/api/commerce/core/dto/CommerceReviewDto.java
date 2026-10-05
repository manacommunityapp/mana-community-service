package com.manacommunity.api.commerce.core.dto;

import com.manacommunity.api.commerce.core.model.CommerceChannel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommerceReviewDto {
    private Long id;
    private Long orderId;
    private Long userId;
    private String userName;
    private CommerceChannel channel;
    private String targetType;
    private String targetId;
    private Integer rating;
    private Integer qualityScore;
    private Integer onTimeScore;
    private String comment;
    private List<String> photos;
    private String createdAt;
}
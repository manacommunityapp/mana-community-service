package com.manacommunity.api.groupbuy.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class DemandItemResponse {

    private Long id;
    private String title;
    private String description;
    private String category;
    private String requestedBy;
    private Integer upvotes;
    private Integer targetUpvotes;
    private Boolean hasUpvoted;
    private String status;
    private LocalDateTime createdAt;
}

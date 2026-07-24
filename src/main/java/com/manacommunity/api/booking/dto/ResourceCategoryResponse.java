package com.manacommunity.api.booking.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResourceCategoryResponse {

    private Long id;
    private String name;
    private String icon;
    private String color;
    private String description;
    private Integer displayOrder;
    private String status;
    private String imageUrl;
    private Long resourceCount;
    private Long communityId;
}

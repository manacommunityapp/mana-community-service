package com.manacommunity.api.marketplace.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketCategoryResponse {

    private Long id;
    private String name;
    private String slug;
    private String icon;
    private Long parentId;
    private String parentName;
    private int sortOrder;
    private boolean active;
}

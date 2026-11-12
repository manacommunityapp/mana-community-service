package com.manacommunity.api.fresh.catalog.dto;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;

@Data @Builder
public class FreshProductAttributeDto {
    private Long id;
    private String attributeKey;
    private String attributeValue;
    private String unit;
    private Integer displayOrder;
    private Boolean isSearchable;
    private Instant createdAt;
}
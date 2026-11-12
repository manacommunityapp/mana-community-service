package com.manacommunity.api.fresh.catalog.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data @Builder
public class FreshProductAttributeRequest {
    private List<AttributeItem> attributes;

    @Data
    public static class AttributeItem {
        private String attributeKey;
        private String attributeValue;
        private String unit;
        private Integer displayOrder;
        private Boolean isSearchable;
    }
}
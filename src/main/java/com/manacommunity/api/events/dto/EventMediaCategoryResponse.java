package com.manacommunity.api.events.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EventMediaCategoryResponse {

    private Long id;
    private String name;
    private int sortOrder;
}

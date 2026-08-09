package com.manacommunity.api.events.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EventMediaDayResponse {

    private Long id;
    private String label;
    private int sortOrder;
}

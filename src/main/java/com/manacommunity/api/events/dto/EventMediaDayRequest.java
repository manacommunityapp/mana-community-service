package com.manacommunity.api.events.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EventMediaDayRequest {

    @NotBlank
    private String label;

    private Integer sortOrder;
}

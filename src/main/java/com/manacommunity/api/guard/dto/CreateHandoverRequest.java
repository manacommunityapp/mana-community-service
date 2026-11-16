package com.manacommunity.api.guard.dto;

import lombok.Data;
import java.util.List;

@Data
public class CreateHandoverRequest {
    private String shiftType;
    private String notes;
    private List<HandoverItemDto> items;

    @Data
    public static class HandoverItemDto {
        private String category;
        private String description;
        private String priority;
        private Boolean resolvedBefore;
    }
}

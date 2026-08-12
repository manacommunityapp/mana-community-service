package com.manacommunity.api.events.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingActionItemDto {
    private String id;
    private String task;
    private String due;
    private String priority;
    private String category;
    private boolean done;
}

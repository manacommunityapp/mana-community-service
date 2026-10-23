package com.manacommunity.api.events.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TicketTypeDto {
    private String id;
    private String name;
    private Object price;
    private Object qty;
    private Object seats;
    private Object capacity;
    private String description;
}

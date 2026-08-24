package com.manacommunity.api.events.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventContactDto {
    private String id;
    private String name;
    private String phone;
    private String email;
    private String role;
    private String notes;
    private Boolean isPrimary;
    private Integer displayOrder;
}

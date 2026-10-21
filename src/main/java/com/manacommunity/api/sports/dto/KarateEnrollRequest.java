package com.manacommunity.api.sports.dto;

import lombok.Data;

@Data
public class KarateEnrollRequest {
    private Long currentBeltId;    // optional — student's current belt
}

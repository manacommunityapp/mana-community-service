package com.manacommunity.api.safety.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PatrolCheckpointResponse {
    private Long id;
    private String name;
    private String location;
    private String qrCode;
    private Integer sequenceOrder;
    private Boolean active;
    private LocalDateTime createdAt;
}

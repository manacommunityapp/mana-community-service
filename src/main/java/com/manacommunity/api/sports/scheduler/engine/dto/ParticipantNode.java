package com.manacommunity.api.sports.scheduler.engine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantNode {
    private String id;
    private String name;
    private String flatNumber;
    private String tower;
    private Integer seed;
    private Double rating; // Elo or tournament ranking points
    private String skillTier; // ADVANCED, INTERMEDIATE, BEGINNER
    private boolean isBye;

    public static ParticipantNode byeNode(String byeId) {
        return ParticipantNode.builder()
                .id(byeId)
                .name("BYE")
                .isBye(true)
                .seed(9999)
                .rating(0.0)
                .build();
    }
}

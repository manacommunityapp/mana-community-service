package com.manacommunity.api.intelligence.dto;

import com.manacommunity.api.intelligence.model.ProfileVisibility;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommunityProfileDto {
    private String id;
    private Long userId;
    private String name;
    private String flatNumber;
    private String tower;
    private String bio;
    private List<String> professions;
    private List<String> skills;
    private List<String> interests;
    private List<String> sports;
    private String availabilityHours;
    private ProfileVisibility visibility;
    private boolean isVerified;
    private String phone; // masked if privacy restricts
    private String email; // masked if privacy restricts
    private int matchScore;
    private int mutualConnections;
}
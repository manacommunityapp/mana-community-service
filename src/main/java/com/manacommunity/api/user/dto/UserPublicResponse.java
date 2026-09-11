package com.manacommunity.api.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Privacy-minimized public user profile for resident directory and member searches.
 * Excludes sensitive government identifiers, DOB, and masks contact info according to privacy preferences.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPublicResponse {
    private Long id;
    private String fullName;
    private String profilePicUrl;
    private String flatNo;
    private String block;
    private String tower;
    private String occupancyStatus;
    private String userType;
    private String phone; // masked or omitted based on privacy settings
    private String email; // masked or omitted based on privacy settings
    private Long communityId;
}

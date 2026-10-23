package com.manacommunity.api.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Lightweight user summary for fast player selection, tournament rosters, and mentions.
 * Only includes minimal identification and contact fields, avoiding heavy role/permission/module joins.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSummaryResponse {
    private Long id;
    private String fullName;
    private String email;
    private String phone;
    private String profilePicUrl;
    private String avatarUrl;
    private String gender;
    private LocalDate dateOfBirth;
    private String flatNo;
    private String block;
    private Long communityId;
}

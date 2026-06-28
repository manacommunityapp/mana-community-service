package com.manacommunity.api.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Lightweight DTO returned for community dropdowns.
 * Exposes only what the signup form needs.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommunityResponse {
    private Long id;
    private String name;
    private String type;   // APARTMENT | COLLEGE | SCHOOL | OFFICE
    private String city;
    private String state;
    private String area;
    private String subtype;
    private String inviteCode;
    private Boolean active;
}

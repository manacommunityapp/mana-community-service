package com.manacommunity.api.mw.dto;

import lombok.Data;

@Data
public class PassportUpdateRequest {
    private String bio;
    private String location;
    private String preferredRoles;
    private String availability;
    private Boolean isPublic;
}

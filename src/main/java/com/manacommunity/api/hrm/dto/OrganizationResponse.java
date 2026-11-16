package com.manacommunity.api.hrm.dto;

import com.manacommunity.api.hrm.enums.HrOrganizationType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class OrganizationResponse {
    private Long id;
    private Long communityId;
    private String organizationName;
    private HrOrganizationType organizationType;
    private String gstNumber;
    private String registrationNumber;
    private String contactEmail;
    private String contactPhone;
    private String address;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

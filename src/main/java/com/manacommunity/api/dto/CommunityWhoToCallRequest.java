package com.manacommunity.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunityWhoToCallRequest {

    @NotBlank(message = "Department or issue category is required")
    @Size(max = 100, message = "Department must not exceed 100 characters")
    private String department;

    @NotBlank(message = "Contact person name is required")
    @Size(max = 100, message = "Contact person must not exceed 100 characters")
    private String contactPerson;

    private Long userId;

    @NotBlank(message = "Primary phone number is required")
    @Size(max = 25, message = "Phone number must not exceed 25 characters")
    private String phoneNumber;

    @Size(max = 25, message = "Secondary phone number must not exceed 25 characters")
    private String secondaryPhone;

    @Size(max = 120, message = "Email must not exceed 120 characters")
    private String email;

    @Size(max = 100, message = "Designation must not exceed 100 characters")
    private String designation;

    @Size(max = 100, message = "Availability must not exceed 100 characters")
    private String availability;

    @Size(max = 100, message = "Location/desk must not exceed 100 characters")
    private String locationOrDesk;

    private String icon;
    private String color;
    private Boolean isEmergency;
    private Integer displayOrder;
    private Boolean isActive;
}

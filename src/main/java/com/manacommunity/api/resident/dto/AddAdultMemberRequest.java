package com.manacommunity.api.resident.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddAdultMemberRequest {

    @NotNull(message = "flatId is required")
    private Long flatId;

    @NotBlank(message = "fullName is required")
    private String fullName;

    @NotBlank(message = "phone is required")
    @Pattern(regexp = "^[0-9]{10,15}$", message = "phone must be 10-15 digits")
    private String phone;

    private String email;

    @NotBlank(message = "relationship is required")
    private String relationship; // SPOUSE, PARENT, SIBLING, CO_OWNER, TENANT, OTHER

    @Builder.Default
    private Boolean allowAppAccess = true;

    private String notes;
}

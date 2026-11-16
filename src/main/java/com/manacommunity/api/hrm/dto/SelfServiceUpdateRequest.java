package com.manacommunity.api.hrm.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SelfServiceUpdateRequest {

    @Email(message = "Invalid email format")
    @Size(max = 150)
    private String personalEmail;

    @Size(max = 15)
    private String phone;

    @Size(max = 100)
    private String emergencyContactName;

    @Size(max = 15)
    private String emergencyContactPhone;
}

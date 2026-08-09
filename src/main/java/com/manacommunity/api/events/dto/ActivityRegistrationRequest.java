package com.manacommunity.api.events.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
public class ActivityRegistrationRequest {
    private Integer headCount;
    private String registrationType;
    private String primaryName;
    @Email
    private String primaryEmail;
    private String primaryPhone;
    private String idempotencyKey;
    private Map<String, Object> customData;
    @Valid
    private List<ParticipantRequest> participants;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParticipantRequest {
        @NotBlank
        private String fullName;
        private Integer age;
        private String gender;
        private String relationship;
        @Email
        private String email;
        private String phone;
        private Map<String, Object> customData;
    }
}

package com.manacommunity.api.socialcircle.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

public class KittyPartyCircleDtos {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateCircleRequest {
        @NotBlank(message = "Circle name is required")
        @Size(min = 3, max = 150, message = "Circle name must be between 3 and 150 characters")
        private String circleName;

        private String description;

        @NotBlank(message = "Category code is required")
        private String categoryCode; // KITTY, LADIES_CLUB, FAMILY, FRIENDS, HOBBY, PROFESSIONAL

        @NotBlank(message = "Privacy type is required")
        private String privacyType; // COMMUNITY_PUBLIC, PRIVATE, INVITE_ONLY

        private Boolean approvalRequired;
        private Integer maxMembers;
        private Boolean allowExternalMembers;
        private Integer maxExternalMembers;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateCircleRequest {
        private String circleName;
        private String description;
        private String categoryCode;
        private String privacyType;
        private Boolean approvalRequired;
        private Integer maxMembers;
        private Boolean allowExternalMembers;
        private Integer maxExternalMembers;
        private String status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CircleResponse {
        private Long id;
        private Long communityId;
        private String circleName;
        private String description;
        private String categoryCode;
        private String privacyType;
        private Boolean approvalRequired;
        private Integer maxMembers;
        private Boolean allowExternalMembers;
        private Integer maxExternalMembers;
        private Long externalMembersCount;
        private Long currentMembersCount;
        private String status;
        private Long createdById;
        private String createdByName;
        private LocalDateTime createdAt;
        private String currentUserRole; // e.g. OWNER, ADMIN, MEMBER or null
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberResponse {
        private Long memberId;
        private Long userId;
        private String fullName;
        private String phone;
        private String roleCode;
        private String membershipStatus;
        private LocalDateTime joinedAt;
        private String flatUnit;
        private Boolean isExternal;
        private Long sponsorUserId;
        private String sponsorName;
        private String externalSocietyName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InviteRequest {
        private Long invitedUserId;
        private String invitedPhone;
        private String invitedFullName;
        private Boolean isExternal;
        private String externalSocietyName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JoinRequestPayload {
        private String requestMessage;
    }
}

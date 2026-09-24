package com.manacommunity.api.dto.admin;

import com.manacommunity.api.model.AppUser;
import lombok.Builder;
import lombok.Data;

import java.time.format.DateTimeFormatter;

@Data
@Builder
public class AdminMemberResponse {
    private Long   id;
    private String name;
    private String email;
    private String mobile;
    private String flatNumber;
    private String tower;
    private String role;
    /** ACTIVE | PENDING | SUSPENDED — derived from kycStatus + isActive */
    private String status;
    private String joinedAt;
    private String verifiedAt;

    // ── Factory ───────────────────────────────────────────────────────
    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public static AdminMemberResponse from(AppUser u) {
        String status;
        if (Boolean.FALSE.equals(u.getIsActive())) {
            status = "SUSPENDED";
        } else if ("VERIFIED".equalsIgnoreCase(u.getKycStatus())) {
            status = "ACTIVE";
        } else {
            status = "PENDING";
        }

        return AdminMemberResponse.builder()
                .id(u.getId())
                .name(u.getFullName())
                .email(u.getEmail())
                .mobile(u.getPhone())
                .flatNumber(u.getFlatNo())
                .tower(u.getBlock())
                .role(u.getRole())
                .status(status)
                .joinedAt(u.getCreatedAt() != null ? u.getCreatedAt().format(FMT) : null)
                .build();
    }
}

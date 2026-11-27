package com.manacommunity.api.staffpayroll.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "domestic_staff_profiles")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class DomesticStaffProfile extends BaseAuditEntity {

    public enum StaffRole {
        MAID, COOK, DRIVER, NANNY, GARDENER, WATCHMAN, TUTOR, PHYSIOTHERAPIST
    }

    @Column(nullable = false) private Long communityId;
    @Column(nullable = false, unique = true) private String staffCode;

    private String fullName;
    private String phone;
    private String aadhaarMasked;
    private String photoUrl;

    @Enumerated(EnumType.STRING) @Column(nullable = false) private StaffRole primaryRole;

    private int totalEmployers;
    @Builder.Default private boolean active = true;
    private String gatePassId;
    private String preferredLanguage;
}

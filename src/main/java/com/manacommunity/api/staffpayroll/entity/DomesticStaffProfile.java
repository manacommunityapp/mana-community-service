package com.manacommunity.api.staffpayroll.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "domestic_staff_profiles")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class DomesticStaffProfile extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public enum StaffRole {
        MAID, COOK, DRIVER, NANNY, GARDENER, WATCHMAN, TUTOR, PHYSIOTHERAPIST
    }

    @Column(nullable = false) private Long communityId;
    @Column(nullable = false, unique = true) private String staffCode;

    private String fullName;
    private String phone;
    private String phoneNumber;
    private String aadhaarMasked;
    private String aadhaarLastFour;
    private String photoUrl;

    @Enumerated(EnumType.STRING) @Column(nullable = false) private StaffRole primaryRole;

    private int totalEmployers;
    @Builder.Default private boolean active = true;
    @Builder.Default private boolean isActive = true;
    private String gatePassId;
    private String gatePassNumber;
    private String preferredLanguage;
    private LocalDate joinedDate;
}

package com.manacommunity.api.guardops.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "guard_profiles")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GuardProfile extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String communityId;
    private String guardName;
    private String guardCode;
    private String phone;
    private String agencyName;
    private String photoUrl;

    @Enumerated(EnumType.STRING)
    private GuardRank rank;

    private String assignedGateId;
    private String assignedGateName;
    private LocalDate joiningDate;
    private boolean biometricRegistered;
    private boolean isActive;

    private int totalShiftsCompleted;
    private int incidentsClosed;
    private int gamificationPoints;

    @Enumerated(EnumType.STRING)
    private GuardTier performanceTier;

    public enum GuardRank { GUARD, SENIOR_GUARD, SHIFT_SUPERVISOR, HEAD_GUARD, SECURITY_MANAGER }
    public enum GuardTier { BRONZE, SILVER, GOLD, PLATINUM }
}

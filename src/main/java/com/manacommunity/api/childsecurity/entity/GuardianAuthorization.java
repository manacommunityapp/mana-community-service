package com.manacommunity.api.childsecurity.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "guardian_authorizations")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GuardianAuthorization extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long childId;
    private String communityId;

    private String guardianName;
    private String guardianPhone;
    private String guardianEmail;
    private String relationship;
    private String photoUrl;

    @Enumerated(EnumType.STRING)
    private GuardianRole role;

    @Enumerated(EnumType.STRING)
    private AuthorizationLevel authLevel;

    private int escalationOrder;
    private boolean ivrEnabled;
    private boolean pushNotificationEnabled;

    private boolean isActive;

    public enum GuardianRole { PRIMARY_PARENT, SECONDARY_PARENT, GRANDPARENT, AUTHORIZED_PICKUP, EMERGENCY_CONTACT }

    public enum AuthorizationLevel {
        FULL_APPROVE, APPROVE_WITH_OTP, VIEW_ONLY, EMERGENCY_ONLY
    }
}

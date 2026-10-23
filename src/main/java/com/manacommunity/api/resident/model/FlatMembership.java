package com.manacommunity.api.resident.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.manacommunity.api.model.common.BaseAuditEntity;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "flat_membership",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_user_flat_membership", columnNames = {"user_id", "flat_id"})
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(value = {"hibernateLazyInitializer", "handler"}, ignoreUnknown = true)
public class FlatMembership extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flat_id", nullable = false)
    private CommunityFlat flat;

    @Column(name = "resident_type", nullable = false, length = 30)
    @Builder.Default
    private String residentType = "OWNER"; // OWNER, TENANT, FAMILY_MEMBER

    @Column(name = "relationship_to_flat", length = 50)
    @Builder.Default
    private String relationshipToFlat = "SELF"; // SELF, SPOUSE, PARENT, CHILD, SIBLING, RELATIVE, OTHER

    @Column(name = "is_primary_resident", nullable = false)
    @Builder.Default
    private Boolean isPrimaryResident = false;

    @Column(name = "app_access_status", nullable = false, length = 30)
    @Builder.Default
    private String appAccessStatus = "FULL_ACCESS"; // FULL_ACCESS, READ_ONLY, INVITED, REVOKED

    @Column(name = "invite_phone", length = 20)
    private String invitePhone;

    @Column(name = "invited_by")
    private Long invitedBy;
}

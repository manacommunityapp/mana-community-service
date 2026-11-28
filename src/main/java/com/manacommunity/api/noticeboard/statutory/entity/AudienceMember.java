package com.manacommunity.api.noticeboard.statutory.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "notice_audience_members")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AudienceMember extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long audienceGroupId;
    private String communityId;

    private String flatNumber;
    private String towerName;
    private String residentName;
    private String email;
    private String phone;
    private Long userId;

    @Enumerated(EnumType.STRING)
    private OwnershipType ownershipType;

    private boolean matchedByRule;

    public enum OwnershipType { OWNER, TENANT, FAMILY_MEMBER, AUTHORIZED_REPRESENTATIVE }
}

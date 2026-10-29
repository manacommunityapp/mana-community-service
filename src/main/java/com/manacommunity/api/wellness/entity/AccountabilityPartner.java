package com.manacommunity.api.wellness.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import com.manacommunity.api.wellness.enums.WellnessEnums.*;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "accountability_partner", schema = "manacommunity")
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountabilityPartner extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "partner_user_id", nullable = false)
    private Long partnerUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "consent_status")
    private ConsentStatus consentStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "consent_type")
    private ConsentType consentType;

    @Column(name = "started_date")
    private LocalDate startedDate;

    @Column(name = "active")
    private Boolean active;
}

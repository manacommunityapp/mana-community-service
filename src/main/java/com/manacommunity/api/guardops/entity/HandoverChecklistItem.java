package com.manacommunity.api.guardops.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "handover_checklist_items")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class HandoverChecklistItem extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long handoverId;

    @Enumerated(EnumType.STRING)
    private ChecklistCategory category;

    private String itemDescription;
    private int count;

    @Enumerated(EnumType.STRING)
    private ItemPriority priority;

    @Enumerated(EnumType.STRING)
    private ItemStatus itemStatus;

    private boolean outgoingConfirmed;
    private boolean incomingAcknowledged;
    private String actionRequired;
    private String remarks;

    public enum ChecklistCategory { VISITORS_INSIDE, UNCOLLECTED_PACKAGES, OPEN_INCIDENTS, EQUIPMENT_STATUS, PENDING_DELIVERIES, VEHICLE_ISSUES, INTERCOM_ALERTS, CUSTOM }
    public enum ItemPriority { CRITICAL, HIGH, MEDIUM, LOW }
    public enum ItemStatus { OPEN, ACKNOWLEDGED, RESOLVED, CARRIED_OVER }
}

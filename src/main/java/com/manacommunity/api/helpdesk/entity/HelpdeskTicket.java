package com.manacommunity.api.helpdesk.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "helpdesk_tickets", indexes = {
    @Index(name = "idx_ticket_community_status", columnList = "community_id, status"),
    @Index(name = "idx_ticket_parent", columnList = "parent_incident_id"),
    @Index(name = "idx_ticket_assignee", columnList = "assigned_technician_id")
})
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class HelpdeskTicket extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(name = "resident_id", nullable = false)
    private Long residentId;

    @Column(name = "flat_no", length = 20)
    private String flatNo;

    @Column(name = "tower", length = 20)
    private String tower;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false, length = 40)
    private String category;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String priority = "MEDIUM";

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "OPEN";

    @Column(name = "escalation_level", nullable = false)
    @Builder.Default
    private Integer escalationLevel = 0;

    @Column(name = "sla_minutes", nullable = false)
    @Builder.Default
    private Integer slaMinutes = 120;

    @Column(name = "sla_deadline")
    private Instant slaDeadline;

    @Column(name = "sla_paused", nullable = false)
    @Builder.Default
    private Boolean slaPaused = false;

    @Column(name = "sla_elapsed_minutes", nullable = false)
    @Builder.Default
    private Integer slaElapsedMinutes = 0;

    @Column(name = "sla_pause_start")
    private Instant slaPauseStart;

    @Column(name = "parent_incident_id")
    private Long parentIncidentId;

    @Column(name = "is_parent_incident", nullable = false)
    @Builder.Default
    private Boolean isParentIncident = false;

    @Column(name = "child_count", nullable = false)
    @Builder.Default
    private Integer childCount = 0;

    @Column(name = "assigned_technician_id")
    private Long assignedTechnicianId;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "resolution_notes", length = 1000)
    private String resolutionNotes;
}

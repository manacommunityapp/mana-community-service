package com.manacommunity.api.helpdesk.entity;

import com.manacommunity.api.model.Community;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "helpdesk_sla_rule")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketSlaRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Ticket.TicketCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Ticket.TicketPriority priority;

    @Column(name = "resolution_time_hours", nullable = false)
    private int resolutionTimeHours;

    @Column(name = "escalation_level1_hours", nullable = false)
    private int escalationLevel1Hours;

    @Column(name = "escalation_level2_hours", nullable = false)
    private int escalationLevel2Hours;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id")
    private Community community;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

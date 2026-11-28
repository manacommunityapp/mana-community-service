package com.manacommunity.api.helpdesk.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "escalation_rules", uniqueConstraints = {
    @UniqueConstraint(name = "uq_escalation_rule", columnNames = {"community_id", "category", "escalation_level"})
})
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class EscalationRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(nullable = false, length = 40)
    private String category;

    @Column(name = "escalation_level", nullable = false)
    private Integer escalationLevel;

    @Column(name = "escalate_after_minutes", nullable = false)
    private Integer escalateAfterMinutes;

    @Column(name = "escalate_to_role", nullable = false, length = 40)
    private String escalateToRole;

    @Column(name = "pause_during_off_hours", nullable = false)
    @Builder.Default
    private Boolean pauseDuringOffHours = false;

    @Column(name = "off_hours_start")
    @Builder.Default
    private Integer offHoursStart = 22;

    @Column(name = "off_hours_end")
    @Builder.Default
    private Integer offHoursEnd = 7;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
}

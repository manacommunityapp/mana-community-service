package com.manacommunity.api.helpdesk.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;

@Entity
@Table(name = "technicians")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Technician {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "phone", length = 15)
    private String phone;

    @Column(name = "primary_skill", nullable = false, length = 40)
    private String primarySkill;

    @Column(name = "secondary_skills", length = 200)
    private String secondarySkills;

    @Column(name = "current_tower", length = 20)
    private String currentTower;

    @Column(name = "shift_start", nullable = false)
    @Builder.Default
    private LocalTime shiftStart = LocalTime.of(8, 0);

    @Column(name = "shift_end", nullable = false)
    @Builder.Default
    private LocalTime shiftEnd = LocalTime.of(18, 0);

    @Column(name = "active_ticket_count", nullable = false)
    @Builder.Default
    private Integer activeTicketCount = 0;

    @Column(name = "max_concurrent_tickets", nullable = false)
    @Builder.Default
    private Integer maxConcurrentTickets = 5;

    @Column(name = "is_available", nullable = false)
    @Builder.Default
    private Boolean isAvailable = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    public boolean hasSkill(String skill) {
        if (primarySkill.equalsIgnoreCase(skill)) return true;
        if (secondarySkills == null) return false;
        for (String s : secondarySkills.split(",")) {
            if (s.trim().equalsIgnoreCase(skill)) return true;
        }
        return false;
    }

    public boolean isOnShift(LocalTime now) {
        if (shiftStart.isBefore(shiftEnd)) {
            return !now.isBefore(shiftStart) && now.isBefore(shiftEnd);
        }
        return !now.isBefore(shiftStart) || now.isBefore(shiftEnd);
    }

    public boolean hasCapacity() {
        return activeTicketCount < maxConcurrentTickets;
    }
}

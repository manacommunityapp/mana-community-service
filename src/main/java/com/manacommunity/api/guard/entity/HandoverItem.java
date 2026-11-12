package com.manacommunity.api.guard.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "guard_handover_item")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HandoverItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "handover_id", nullable = false)
    private ShiftHandover handover;

    @Column(length = 50)
    private String category;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 20)
    @Builder.Default
    private String priority = "MEDIUM";

    @Column(name = "resolved_before")
    @Builder.Default
    private Boolean resolvedBefore = false;
}

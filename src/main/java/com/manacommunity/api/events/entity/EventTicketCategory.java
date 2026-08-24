package com.manacommunity.api.events.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "event_ticket_categories")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventTicketCategory extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    private CommunityEvent event;

    @Column(name = "community_id")
    private Long communityId;

    @Column(name = "ticket_code", length = 100)
    private String ticketCode;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false)
    @Builder.Default
    private Double price = 0.0;

    @Column(name = "capacity")
    private Integer capacity;

    @Column(name = "seats")
    private Integer seats;

    @Column(length = 1000)
    private String description;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}

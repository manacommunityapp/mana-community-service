package com.manacommunity.api.serviceplatform.interiors.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name="interior_payment_event")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InteriorPaymentEvent {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="milestone_id",nullable=false) private InteriorPaymentMilestone milestone;
    @Column(name="actor_user_id",nullable=false) private Long actorUserId;
    @Column(name="event_type",nullable=false,length=32) private String eventType;
    @Column(columnDefinition="TEXT") private String reference;
    @Column(name="created_at",nullable=false,updatable=false) private LocalDateTime createdAt;
    @PrePersist void onCreate(){createdAt=LocalDateTime.now();}
}

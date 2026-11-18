package com.manacommunity.api.serviceplatform.interiors.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "interior_saved_designer", uniqueConstraints = @UniqueConstraint(
        name = "uq_interior_saved_designer", columnNames = {"user_id", "provider_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InteriorSavedDesigner {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "provider_id", nullable = false)
    private Long providerId;

    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

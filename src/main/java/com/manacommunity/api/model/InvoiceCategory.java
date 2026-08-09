package com.manacommunity.api.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "invoice_category")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "community_id")
    private Long communityId;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 60)
    private String code;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

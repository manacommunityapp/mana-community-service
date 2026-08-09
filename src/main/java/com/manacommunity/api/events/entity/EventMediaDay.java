package com.manacommunity.api.events.entity;

import com.manacommunity.api.model.Community;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "event_media_day")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventMediaDay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Community community;

    @Column(nullable = false, length = 100)
    private String label;

    @Column(name = "sort_order")
    @Builder.Default
    private int sortOrder = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

package com.manacommunity.api.safety.model;

import com.manacommunity.api.model.Community;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "patrol_checkpoint", indexes = {
        @Index(name = "idx_patrol_checkpoint_community", columnList = "community_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatrolCheckpoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 200)
    private String location;

    @Column(name = "qr_code", length = 200)
    private String qrCode;

    @Column(name = "sequence_order")
    private Integer sequenceOrder;

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}

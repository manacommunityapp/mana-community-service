package com.manacommunity.api.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cpos_crm_inbound_config")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CposCrmInboundConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @Column(name = "api_key", nullable = false, length = 64, unique = true)
    private String apiKey;

    @Column(name = "agent_pool", columnDefinition = "TEXT")
    private String agentPool;

    @Column(name = "rr_index", nullable = false)
    @Builder.Default
    private int rrIndex = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}

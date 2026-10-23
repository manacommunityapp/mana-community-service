package com.manacommunity.api.cfbos.approval.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.manacommunity.api.cfbos.shared.enums.ApprovalState;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "cfbos_approval_action")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalAction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    @JsonIgnore
    private ApprovalRequest request;

    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 20)
    private ApprovalState action;

    @Column(name = "actor_id", nullable = false)
    private Long actorId;

    @Column(name = "acted_at", nullable = false)
    private LocalDateTime actedAt;

    @Column(name = "comments", columnDefinition = "TEXT")
    private String comments;

    @PrePersist
    protected void onCreate() {
        if (actedAt == null) actedAt = LocalDateTime.now();
    }
}

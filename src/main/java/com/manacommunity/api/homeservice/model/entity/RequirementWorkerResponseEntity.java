package com.manacommunity.api.homeservice.model.entity;

import com.manacommunity.api.homeservice.model.enums.HomeServiceBidStatus;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "requirement_worker_responses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequirementWorkerResponseEntity {
    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "request_id", nullable = false, length = 64)
    private String requestId;

    @Column(name = "worker_id", nullable = false, length = 64)
    private String workerId;

    @Column(name = "worker_name", length = 150)
    private String workerName;

    @Column(name = "worker_phone", length = 20)
    private String workerPhone;

    @Column(name = "proposed_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal proposedPrice;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private HomeServiceBidStatus status = HomeServiceBidStatus.PENDING;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}

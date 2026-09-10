package com.manacommunity.api.marketplace.entity;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "marketplace_product_requests", indexes = {
    @Index(name = "idx_mkt_req_comm", columnList = "community_id"),
    @Index(name = "idx_mkt_req_status", columnList = "status"),
    @Index(name = "idx_mkt_req_category", columnList = "category")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketProductRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_title", nullable = false, length = 150)
    private String requestTitle;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(name = "target_budget", precision = 12, scale = 2)
    private BigDecimal targetBudget;

    @Column(name = "needed_by_date")
    private LocalDate neededByDate;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private UrgencyLevel urgency = UrgencyLevel.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private RequestStatus status = RequestStatus.OPEN;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private AppUser requester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @OneToMany(mappedBy = "productRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MarketRequestOffer> sellerOffers = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = RequestStatus.OPEN;
        if (urgency == null) urgency = UrgencyLevel.MEDIUM;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum UrgencyLevel { LOW, MEDIUM, HIGH, IMMEDIATE }
    public enum RequestStatus { OPEN, FULFILLED, CLOSED, EXPIRED }
}

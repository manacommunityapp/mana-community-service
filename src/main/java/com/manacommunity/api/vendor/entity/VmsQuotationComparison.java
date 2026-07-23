package com.manacommunity.api.vendor.entity;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "vms_quotation_comparisons")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VmsQuotationComparison {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_request_id", nullable = false)
    private VmsPurchaseRequest purchaseRequest;

    @Column(length = 200)
    private String title;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "IN_PROGRESS";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selected_quotation_id")
    private VmsQuotation selectedQuotation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selected_by")
    private AppUser selectedBy;

    @Column(name = "selected_at")
    private LocalDateTime selectedAt;

    @Column(name = "ai_recommendation", length = 2000)
    private String aiRecommendation;

    @Column(length = 1000)
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private AppUser createdByUser;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }

    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}

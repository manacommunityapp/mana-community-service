package com.manacommunity.api.market.bazaar.entity;

import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "market_application", indexes = {
        @Index(name = "idx_market_application_vendor", columnList = "applicant_id, requested_at"),
        @Index(name = "idx_market_application_event_status", columnList = "event_id, status")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MarketApplication {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private CommunityMarketEvent event;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stall_id", nullable = false)
    private MarketStall stall;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "applicant_id", nullable = false)
    private AppUser applicant;

    @Column(name = "vendor_name", nullable = false, length = 150) private String vendorName;
    @Column(name = "business_name", nullable = false, length = 150) private String businessName;
    @Column(nullable = false, length = 100) private String category;
    @Column(nullable = false, length = 30) private String phone;
    @Column(nullable = false, columnDefinition = "TEXT") private String products;
    @Column(name = "compliance_documents", columnDefinition = "TEXT") private String complianceDocuments;
    @Column(nullable = false, length = 24) @Builder.Default private String status = "SUBMITTED";
    @Column(name = "rejection_reason", length = 1000) private String rejectionReason;
    @Column(name = "requested_at", nullable = false, updatable = false) private LocalDateTime requestedAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;

    @PrePersist protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        requestedAt = now;
        updatedAt = now;
    }
    @PreUpdate protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}

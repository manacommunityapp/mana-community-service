package com.manacommunity.api.market.bazaar.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "market_stall", uniqueConstraints = @UniqueConstraint(name = "uq_market_stall_number", columnNames = {"event_id", "stall_number"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MarketStall {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private CommunityMarketEvent event;

    @Column(name = "stall_number", nullable = false, length = 40) private String stallNumber;
    @Column(length = 100) private String zone;
    @Column(name = "package_name", nullable = false, length = 100) private String packageName;
    @Column(columnDefinition = "TEXT") private String description;
    @Column(length = 100) private String size;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal price;
    @Column(nullable = false, length = 5) @Builder.Default private String currency = "INR";
    @Column(columnDefinition = "TEXT") private String facilities;
    @Column(name = "allowed_categories", columnDefinition = "TEXT") private String allowedCategories;
    @Column(name = "floor_row") private Integer floorRow;
    @Column(name = "floor_column") private Integer floorColumn;
    @Column(nullable = false, length = 20) @Builder.Default private String status = "AVAILABLE";
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;

    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); }
}


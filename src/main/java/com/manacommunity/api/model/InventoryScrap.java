package com.manacommunity.api.model;

import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Permanent record of every scrap operation. Unlike the stock quant adjustment
 * (which only shows the net effect), this table preserves who scrapped what,
 * when, why, and from which location — useful for audits and waste analysis.
 */
@Entity
@Table(name = "inventory_scrap", indexes = {
    @Index(name = "idx_inv_scrap_product", columnList = "product_id"),
    @Index(name = "idx_inv_scrap_state",   columnList = "state"),
})
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"product", "lot", "sourceLocation", "scrapLocation", "scrappedByUser"})
public class InventoryScrap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private InventoryProduct product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lot_id")
    private InventoryLot lot;

    /** Location the stock was scrapped FROM. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private InventoryLocation sourceLocation;

    /** The scrap location it was moved TO. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scrap_location_id", nullable = false)
    private InventoryLocation scrapLocation;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal quantity;

    @Column(length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ScrapState state = ScrapState.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scrapped_by")
    private AppUser scrappedByUser;

    @Column(name = "scrapped_at")
    private LocalDateTime scrappedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); }

    public enum ScrapState { DRAFT, DONE }
}

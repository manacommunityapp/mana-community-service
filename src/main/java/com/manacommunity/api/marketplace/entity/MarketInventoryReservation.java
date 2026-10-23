package com.manacommunity.api.marketplace.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "market_inventory_reservations", indexes = {
    @Index(name = "idx_mkt_inv_res_listing", columnList = "listing_id"),
    @Index(name = "idx_mkt_inv_res_status", columnList = "status"),
    @Index(name = "idx_mkt_inv_res_expiry", columnList = "reserved_until")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketInventoryReservation extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "listing_id", nullable = false)
    private Long listingId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "order_number", length = 100)
    private String orderNumber;

    @Column(name = "quantity", nullable = false)
    @Builder.Default
    private Integer quantity = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private ReservationStatus status = ReservationStatus.RESERVED;

    @Column(name = "reserved_until", nullable = false)
    private LocalDateTime reservedUntil;

    public enum ReservationStatus {
        RESERVED,
        COMMITTED,
        EXPIRED,
        RELEASED
    }
}

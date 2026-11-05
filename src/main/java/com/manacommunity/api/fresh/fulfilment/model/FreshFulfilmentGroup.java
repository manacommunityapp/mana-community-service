package com.manacommunity.api.fresh.fulfilment.model;

import com.manacommunity.api.commerce.core.model.CommerceOrder;
import com.manacommunity.api.fresh.catalog.model.DeliveryMode;
import com.manacommunity.api.fresh.catalog.model.FulfilmentOwnerType;
import com.manacommunity.api.fresh.city.model.FreshFulfilmentLocation;
import com.manacommunity.api.vendor.entity.VmsVendor;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "fresh_fulfilment_group", schema = "manacommunity")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FreshFulfilmentGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_reference", unique = true, nullable = false, length = 50)
    private String groupReference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private CommerceOrder order;

    @Column(name = "group_number", nullable = false)
    @Builder.Default
    private Integer groupNumber = 1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id")
    private VmsVendor vendor;

    @Enumerated(EnumType.STRING)
    @Column(name = "fulfilment_owner_type", nullable = false, length = 50)
    private FulfilmentOwnerType fulfilmentOwnerType;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_mode", nullable = false, length = 50)
    private DeliveryMode deliveryMode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private FulfilmentGroupStatus status = FulfilmentGroupStatus.CREATED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fulfilment_location_id")
    private FreshFulfilmentLocation fulfilmentLocation;

    @Column(name = "target_delivery_time")
    private Instant targetDeliveryTime;

    @Column(name = "dispatched_at")
    private Instant dispatchedAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "tracking_reference", length = 100)
    private String trackingReference;

    @Column(name = "handover_otp", length = 10)
    private String handoverOtp;

    @Column(name = "qr_code_token", length = 255)
    private String qrCodeToken;

    @Column(name = "subtotal_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal subtotalAmount = BigDecimal.ZERO;

    @Column(name = "total_weight_g", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalWeightG = BigDecimal.ZERO;

    @Column(name = "requires_cold_chain", nullable = false)
    @Builder.Default
    private Boolean requiresColdChain = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "dietary_category", nullable = false, length = 30)
    @Builder.Default
    private DietaryCategory dietaryCategory = DietaryCategory.VEGETARIAN;

    @Column(name = "failed_otp_attempts", nullable = false)
    @Builder.Default
    private Integer failedOtpAttempts = 0;

    @Column(name = "is_handover_locked", nullable = false)
    @Builder.Default
    private Boolean isHandoverLocked = false;

    @Column(name = "metadata_json", columnDefinition = "JSONB")
    private String metadataJson;

    @OneToMany(mappedBy = "fulfilmentGroup", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<FreshFulfilmentItem> items = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

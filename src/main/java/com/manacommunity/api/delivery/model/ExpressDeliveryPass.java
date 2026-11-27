package com.manacommunity.api.delivery.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "express_delivery_passes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpressDeliveryPass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false, length = 64)
    private String orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "partner", nullable = false, length = 32)
    private DeliveryPartner partner;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(name = "flat_number", nullable = false, length = 64)
    private String flatNumber;

    @Column(name = "resident_name", length = 128)
    private String residentName;

    @Column(name = "resident_phone", length = 32)
    private String residentPhone;

    @Column(name = "rider_name", nullable = false, length = 128)
    private String riderName;

    @Column(name = "rider_phone", length = 32)
    private String riderPhone;

    @Column(name = "vehicle_number", length = 32)
    private String vehicleNumber;

    @Column(name = "rider_photo_url", length = 512)
    private String riderPhotoUrl;

    @Column(name = "entry_pass_code", nullable = false, length = 32, unique = true)
    private String entryPassCode;

    @Column(name = "barcode_payload", nullable = false, length = 128)
    private String barcodePayload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    @Builder.Default
    private ExpressPassStatus status = ExpressPassStatus.PRE_APPROVED;

    @Column(name = "leave_at_gate", nullable = false)
    @Builder.Default
    private Boolean leaveAtGate = false;

    @Column(name = "gate_instructions", length = 512)
    private String gateInstructions;

    @Column(name = "items_summary", length = 256)
    private String itemsSummary;

    @Column(name = "valid_from", nullable = false)
    private LocalDateTime validFrom;

    @Column(name = "valid_until", nullable = false)
    private LocalDateTime validUntil;

    @Column(name = "checked_in_at")
    private LocalDateTime checkedInAt;

    @Column(name = "checked_out_at")
    private LocalDateTime checkedOutAt;

    @Column(name = "entry_gate", length = 64)
    private String entryGate;

    @Column(name = "exit_gate", length = 64)
    private String exitGate;

    @Column(name = "manifest_id")
    private Long manifestId;

    @Column(name = "delivery_outcome", length = 32)
    private String deliveryOutcome;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

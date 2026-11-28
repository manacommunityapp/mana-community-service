package com.manacommunity.api.delivery.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "resident_delivery_preferences", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"community_id", "flat_number"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResidentDeliveryPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(name = "flat_number", nullable = false, length = 64)
    private String flatNumber;

    @Column(name = "auto_approve_swiggy", nullable = false)
    @Builder.Default
    private Boolean autoApproveSwiggy = true;

    @Column(name = "auto_approve_zomato", nullable = false)
    @Builder.Default
    private Boolean autoApproveZomato = true;

    @Column(name = "auto_approve_blinkit", nullable = false)
    @Builder.Default
    private Boolean autoApproveBlinkit = true;

    @Column(name = "auto_approve_zepto", nullable = false)
    @Builder.Default
    private Boolean autoApproveZepto = true;

    @Column(name = "auto_approve_amazon", nullable = false)
    @Builder.Default
    private Boolean autoApproveAmazon = true;

    @Column(name = "auto_approve_other", nullable = false)
    @Builder.Default
    private Boolean autoApproveOther = true;

    @Column(name = "default_leave_at_gate", nullable = false)
    @Builder.Default
    private Boolean defaultLeaveAtGate = false;

    @Column(name = "delivery_instructions", length = 512)
    private String deliveryInstructions;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

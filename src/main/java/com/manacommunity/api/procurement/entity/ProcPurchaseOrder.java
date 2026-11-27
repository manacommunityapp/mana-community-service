package com.manacommunity.api.procurement.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "proc_purchase_order")
@Getter @Setter @NoArgsConstructor
public class ProcPurchaseOrder extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long communityId;

    @Column(nullable = false, unique = true)
    private String poNumber;

    @Column(nullable = false)
    private String vendorName;

    private String vendorGstin;

    @Column(nullable = false)
    private String category; // ELEVATOR, DG_SET, STP, HOUSEKEEPING, SECURITY, PLUMBING, ELECTRICAL

    @Column(nullable = false)
    private BigDecimal totalAmount;

    private BigDecimal gstAmount;
    private BigDecimal netPayable;

    @Column(nullable = false)
    private String currency = "INR";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PoStatus status = PoStatus.DRAFT;

    private String description;
    private LocalDate expectedDeliveryDate;
    private String raisedBy;

    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProcPoLineItem> lineItems = new ArrayList<>();

    public enum PoStatus {
        DRAFT, PENDING_APPROVAL, APPROVED, PARTIALLY_RECEIVED, FULLY_RECEIVED, CANCELLED
    }
}

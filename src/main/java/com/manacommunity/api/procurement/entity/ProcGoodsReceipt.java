package com.manacommunity.api.procurement.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "proc_goods_receipt")
@Getter @Setter @NoArgsConstructor
public class ProcGoodsReceipt extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String grnNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_order_id", nullable = false)
    private ProcPurchaseOrder purchaseOrder;

    @Column(nullable = false)
    private Long communityId;

    @Column(nullable = false)
    private int quantityReceived;

    private int quantityRejected;
    private String rejectionReason;

    @Column(nullable = false)
    private BigDecimal receivedValue;

    private String receivedBy;
    private LocalDateTime receivedAt;
    private String inspectionNotes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GrnStatus status = GrnStatus.PENDING_INSPECTION;

    public enum GrnStatus {
        PENDING_INSPECTION, INSPECTED_OK, INSPECTED_PARTIAL, REJECTED
    }
}

package com.manacommunity.api.procurement.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "proc_vendor_invoice")
@Getter @Setter @NoArgsConstructor
public class ProcVendorInvoice extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String invoiceNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_order_id", nullable = false)
    private ProcPurchaseOrder purchaseOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "goods_receipt_id")
    private ProcGoodsReceipt goodsReceipt;

    @Column(nullable = false)
    private Long communityId;

    @Column(nullable = false)
    private BigDecimal invoiceAmount;

    private BigDecimal gstAmount;
    private BigDecimal tdsDeducted;
    private BigDecimal netPayable;

    private LocalDate invoiceDate;
    private LocalDate dueDate;
    private String vendorBankAccount;
    private String vendorIfsc;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvoiceStatus status = InvoiceStatus.PENDING_MATCH;

    @Enumerated(EnumType.STRING)
    private MatchResult matchResult;

    private String matchDiscrepancy;

    public enum InvoiceStatus {
        PENDING_MATCH, MATCHED, MATCH_FAILED, PENDING_APPROVAL, APPROVED, PAID, REJECTED
    }

    public enum MatchResult {
        FULL_MATCH, QUANTITY_MISMATCH, AMOUNT_MISMATCH, BOTH_MISMATCH, NO_GRN
    }
}

package com.manacommunity.api.cfbos.payment.entity;
import com.manacommunity.api.cfbos.payment.enums.PaymentMethodType;
import com.manacommunity.api.cfbos.payment.enums.PaymentStatus;
import com.manacommunity.api.cfbos.shared.entity.CfbosBaseFields;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cfbos_payment")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CfbosPayment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_number", nullable = false, unique = true, length = 30)
    private String paymentNumber;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Column(name = "resident_id", nullable = false)
    private Long residentId;

    @Column(name = "property_id", nullable = false)
    private Long propertyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20)
    private PaymentMethodType paymentMethod;

    @Column(name = "payment_mode", nullable = false, length = 10)
    @Builder.Default
    private String paymentMode = "ONLINE";

    @Column(name = "amount", precision = 18, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "applied_amount", precision = 18, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal appliedAmount = BigDecimal.ZERO;

    @Column(name = "unapplied_amount", precision = 18, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal unappliedAmount = BigDecimal.ZERO;

    @Column(name = "gateway_txn_id")
    private Long gatewayTxnId;

    @Column(name = "gateway_reference", length = 100)
    private String gatewayReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.INITIATED;

    @Column(name = "receipt_id")
    private Long receiptId;

    @Column(name = "journal_entry_id")
    private Long journalEntryId;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CfbosPaymentLine> lines = new ArrayList<>();

    @Embedded
    @Builder.Default
    private CfbosBaseFields baseFields = new CfbosBaseFields();

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

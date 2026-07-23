package com.manacommunity.api.vendor.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "vms_work_order_signatures")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VmsWorkOrderSignature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_order_id", nullable = false)
    private VmsWorkOrder workOrder;

    @Column(name = "signer_type", nullable = false, length = 20)
    private String signerType;

    @Column(name = "signer_name", nullable = false, length = 150)
    private String signerName;

    @Column(name = "signature_url", nullable = false, length = 500)
    private String signatureUrl;

    @Column(name = "signed_at", nullable = false)
    private LocalDateTime signedAt;

    @PrePersist
    protected void onCreate() { if (signedAt == null) signedAt = LocalDateTime.now(); }
}

package com.manacommunity.api.serviceplatform.interiors.entity;

import com.manacommunity.api.serviceplatform.entity.ServiceProvider;
import com.manacommunity.api.serviceplatform.entity.ServiceRequest;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity @Table(name="interior_payment_milestone",indexes=@Index(name="idx_interior_payment_project",columnList="service_request_id,due_date,id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InteriorPaymentMilestone {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="service_request_id",nullable=false) private ServiceRequest serviceRequest;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="provider_id",nullable=false) private ServiceProvider provider;
    @Column(nullable=false,length=180) private String title;
    @Column(columnDefinition="TEXT") private String description;
    @Column(nullable=false,precision=12,scale=2) private BigDecimal amount;
    @Column(name="due_date") private LocalDate dueDate;
    @Column(nullable=false,length=24) @Builder.Default private String status="SCHEDULED";
    @Column(name="payment_gateway",length=24) private String paymentGateway;
    @Column(name="razorpay_order_id",length=60) private String razorpayOrderId;
    @Column(name="razorpay_payment_id",length=60) private String razorpayPaymentId;
    @Column(name="cashfree_mana_order_id",length=60) private String cashfreeManaOrderId;
    @Column(name="authorized_at") private LocalDateTime authorizedAt;
    @Column(name="released_at") private LocalDateTime releasedAt;
    @Column(name="payout_reference",length=120) private String payoutReference;
    @Column(name="invoice_reference",length=120) private String invoiceReference;
    @Column(name="invoice_url",columnDefinition="TEXT") private String invoiceUrl;
    @Column(name="created_at",nullable=false,updatable=false) private LocalDateTime createdAt;
    @Column(name="updated_at",nullable=false) private LocalDateTime updatedAt;
    @PrePersist void onCreate(){createdAt=LocalDateTime.now();updatedAt=createdAt;}
    @PreUpdate void onUpdate(){updatedAt=LocalDateTime.now();}
}

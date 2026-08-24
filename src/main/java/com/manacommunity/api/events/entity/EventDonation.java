package com.manacommunity.api.events.entity;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "event_donation")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventDonation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private EventCommunity event;

    @Column(name = "donor_name", nullable = false, length = 200)
    private String donorName;

    @Column(name = "donor_email", length = 200)
    private String donorEmail;

    @Column(name = "donor_phone", length = 50)
    private String donorPhone;

    @Column(nullable = false)
    private Double amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 30)
    @Builder.Default
    private PaymentMethod paymentMethod = PaymentMethod.CASH;

    @Column(name = "transaction_ref", length = 100)
    private String transactionRef;

    @Column(length = 500)
    private String note;

    @Builder.Default
    private boolean anonymous = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by", nullable = false)
    private AppUser recordedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum PaymentMethod {
        CASH, UPI, BANK_TRANSFER, CHEQUE, ONLINE
    }
}

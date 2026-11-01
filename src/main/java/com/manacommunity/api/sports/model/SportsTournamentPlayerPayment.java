package com.manacommunity.api.sports.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.model.FamilyMember;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "sports_tournament_player_payment",
       indexes = {
           @Index(name = "idx_tourn_payment_tourn_id", columnList = "tournament_id"),
           @Index(name = "idx_tourn_payment_player_key", columnList = "player_key")
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SportsTournamentPlayerPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "sportsEvents"})
    private SportsTournament tournament;

    @Column(name = "player_key", nullable = false, length = 100)
    private String playerKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "family_member_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private FamilyMember familyMember;

    @Column(name = "player_name", nullable = false, length = 150)
    private String playerName;

    @Column(name = "flat_number", length = 50)
    private String flatNumber;

    @Column(length = 50)
    private String phone;

    @Column(length = 150)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", length = 30)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PAID;

    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal amount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_mode", length = 30)
    private PaymentMode paymentMode;

    @Column(name = "transaction_ref", length = 100)
    private String transactionRef;

    @Column(length = 500)
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "marked_by_user_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private AppUser markedByUser;

    @Column(name = "marked_by_user_name", length = 150)
    private String markedByUserName;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (paidAt == null) {
            paidAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum PaymentStatus {
        PAID, UNPAID, WAIVED, REFUNDED
    }

    public enum PaymentMode {
        CASH, UPI, BANK_TRANSFER, ONLINE, WAIVED
    }
}

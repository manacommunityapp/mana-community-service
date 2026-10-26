package com.manacommunity.api.model;

import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "khata_parties")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KhataParty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private KhataType khataType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PartyType partyType;

    @Column(nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "total_gave", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal totalGave = BigDecimal.ZERO;

    @Column(name = "total_got", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal totalGot = BigDecimal.ZERO;

    @Column(length = 255)
    private String notes;

    @Column(name = "last_activity")
    private LocalDateTime lastActivity;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum KhataType {
        VENDOR, PERSONAL
    }

    public enum PartyType {
        CUSTOMER, SUPPLIER, FRIEND, FAMILY
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        lastActivity = LocalDateTime.now();
    }
}

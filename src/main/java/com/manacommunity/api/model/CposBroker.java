package com.manacommunity.api.model;

import com.manacommunity.api.model.Community;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cpos_broker")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CposBroker {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @Column(name = "broker_name", nullable = false, length = 255)
    private String brokerName;

    @Column(name = "firm_name", length = 255)
    private String firmName;

    @Column(length = 50)
    private String phone;

    @Column(length = 255)
    private String email;

    @Column(name = "commission_rate", nullable = false, precision = 4, scale = 2)
    @Builder.Default
    private BigDecimal commissionRate = new BigDecimal("2.00");

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "broker", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<CposBrokerCommission> commissions = new ArrayList<>();
}

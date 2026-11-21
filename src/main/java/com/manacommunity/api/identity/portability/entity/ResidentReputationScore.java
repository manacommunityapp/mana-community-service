package com.manacommunity.api.identity.portability.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "resident_reputation_scores")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResidentReputationScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "resident_id", nullable = false)
    private Long residentId;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(name = "score", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal score = BigDecimal.valueOf(50.00);

    @Column(name = "payment_score", precision = 5, scale = 2)
    private BigDecimal paymentScore;

    @Column(name = "participation", precision = 5, scale = 2)
    private BigDecimal participation;

    @Column(name = "tenure_months", nullable = false)
    @Builder.Default
    private Integer tenureMonths = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "badges", columnDefinition = "jsonb")
    private List<Map<String, Object>> badges;

    @Column(name = "computed_at", nullable = false)
    @Builder.Default
    private Instant computedAt = Instant.now();
}

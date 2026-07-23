package com.manacommunity.api.vendor.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "vms_rating_criteria")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VmsRatingCriteria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rating_id", nullable = false)
    private VmsRating rating;

    @Column(name = "criteria_name", nullable = false, length = 100)
    private String criteriaName;

    @Column(nullable = false, precision = 3, scale = 2)
    private BigDecimal score;
}

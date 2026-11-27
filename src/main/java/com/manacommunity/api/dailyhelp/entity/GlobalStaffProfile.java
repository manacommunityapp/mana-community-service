package com.manacommunity.api.dailyhelp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "global_staff_profile", schema = "manacommunity")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GlobalStaffProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "staff_uuid", nullable = false, unique = true, length = 64)
    private String staffUuid;

    @Column(name = "phone_hash", nullable = false, unique = true, length = 64)
    private String phoneHash;

    @Column(name = "masked_phone", nullable = false, length = 20)
    private String maskedPhone;

    @Column(name = "display_name", nullable = false, length = 120)
    private String displayName;

    @Column(name = "primary_category", nullable = false, length = 30)
    private String primaryCategory;

    @Builder.Default
    @Column(name = "aggregate_rating", nullable = false, precision = 3, scale = 2)
    private BigDecimal aggregateRating = new BigDecimal("5.00");

    @Builder.Default
    @Column(name = "rating_sample_size", nullable = false)
    private Integer ratingSampleSize = 0;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}

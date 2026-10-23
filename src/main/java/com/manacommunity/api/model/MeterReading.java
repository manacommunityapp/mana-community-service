package com.manacommunity.api.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "meter_readings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeterReading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meter_id", nullable = false)
    private SmartMeter meter;

    @Column(name = "reading_value", nullable = false, precision = 14, scale = 2)
    private BigDecimal readingValue;

    @Column(name = "reading_date", nullable = false)
    private LocalDate readingDate;

    @Column(precision = 14, scale = 2)
    private BigDecimal consumption;

    @Column(length = 20)
    private String unit;

    @Column(name = "billed_amount", precision = 14, scale = 2)
    private BigDecimal billedAmount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

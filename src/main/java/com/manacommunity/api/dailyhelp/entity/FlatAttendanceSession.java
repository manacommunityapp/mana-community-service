package com.manacommunity.api.dailyhelp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "flat_attendance_session", schema = "manacommunity",
        uniqueConstraints = @UniqueConstraint(name = "uq_flat_attendance_session", columnNames = {"contract_id", "work_date"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlatAttendanceSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", nullable = false)
    private FlatStaffContract contract;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    @Column(name = "attendance_status", nullable = false, length = 20)
    private String attendanceStatus; // PRESENT, ABSENT, HALF_DAY, LEAVE_PAID, GATE_ABSENT

    @Column(name = "scheduled_minutes", nullable = false)
    private Integer scheduledMinutes;

    @Builder.Default
    @Column(name = "society_dwell_minutes")
    private Integer societyDwellMinutes = 0;

    @Builder.Default
    @Column(name = "pro_rata_multiplier", nullable = false, precision = 3, scale = 2)
    private BigDecimal proRataMultiplier = new BigDecimal("1.00");

    @Column(name = "resident_override_status", length = 20)
    private String residentOverrideStatus;

    @Column(name = "override_reason", length = 255)
    private String overrideReason;

    @Builder.Default
    @Column(name = "reconciled_at", nullable = false)
    private LocalDateTime reconciledAt = LocalDateTime.now();
}

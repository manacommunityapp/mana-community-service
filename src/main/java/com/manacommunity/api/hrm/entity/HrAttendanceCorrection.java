package com.manacommunity.api.hrm.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalTime;
import java.time.LocalDateTime;

@Entity
@Table(name = "hr_attendance_corrections")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HrAttendanceCorrection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attendance_id", nullable = false)
    private HrAttendance attendance;

    @Column(name = "corrected_check_in")
    private LocalTime correctedCheckIn;

    @Column(name = "corrected_check_out")
    private LocalTime correctedCheckOut;

    @Column(name = "corrected_status", length = 30)
    private String correctedStatus;

    @Column(name = "reason", nullable = false, length = 500)
    private String reason;

    @Column(name = "corrected_by", nullable = false)
    private Long correctedBy;

    @Column(name = "original_check_in")
    private LocalTime originalCheckIn;

    @Column(name = "original_check_out")
    private LocalTime originalCheckOut;

    @Column(name = "original_status", length = 30)
    private String originalStatus;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

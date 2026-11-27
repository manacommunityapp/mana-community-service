package com.manacommunity.api.communityops.attendance;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "staff_shift")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffShift {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(name = "staff_user_id", nullable = false)
    private Long staffUserId;

    @Column(name = "shift_date", nullable = false)
    private LocalDate shiftDate;

    @Column(name = "expected_start", nullable = false)
    private LocalTime expectedStart;

    @Column(name = "expected_end", nullable = false)
    private LocalTime expectedEnd;

    @Column(name = "actual_start")
    private Instant actualStart;

    @Column(name = "actual_end")
    private Instant actualEnd;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttendanceStatus status = AttendanceStatus.ABSENT;

    @Column(name = "marked_by")
    private Long markedBy;

    private String notes;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}

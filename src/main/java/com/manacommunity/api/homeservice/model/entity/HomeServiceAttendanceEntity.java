package com.manacommunity.api.homeservice.model.entity;

import com.manacommunity.api.homeservice.model.enums.HomeServiceAttendanceStatus;
import com.manacommunity.api.homeservice.model.enums.HomeServiceMarkedBy;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "service_attendance_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HomeServiceAttendanceEntity {
    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "booking_id", nullable = false, length = 64)
    private String bookingId;

    @Column(name = "worker_id", nullable = false, length = 64)
    private String workerId;

    @Column(name = "resident_user_id", nullable = false, length = 64)
    private String residentUserId;

    @Column(name = "service_date", nullable = false)
    private LocalDate serviceDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private HomeServiceAttendanceStatus status = HomeServiceAttendanceStatus.COMPLETED;

    @Column(name = "in_time")
    private LocalTime inTime;

    @Column(name = "out_time")
    private LocalTime outTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "marked_by", nullable = false, length = 30)
    @Builder.Default
    private HomeServiceMarkedBy markedBy = HomeServiceMarkedBy.RESIDENT;

    @Column(name = "verification_pin", length = 10)
    private String verificationPin;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}

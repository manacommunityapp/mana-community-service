package com.manacommunity.api.staff.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "staff_leave")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffLeave {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(name = "staff_id", nullable = false)
    private Long staffId;

    @Column(name = "resident_user_id", nullable = false)
    private Long residentUserId;

    @Column(name = "leave_date", nullable = false)
    private LocalDate leaveDate;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(length = 20, nullable = false)
    @Builder.Default
    private String status = "APPROVED";

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}

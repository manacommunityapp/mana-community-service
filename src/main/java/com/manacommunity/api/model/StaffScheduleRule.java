package com.manacommunity.api.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "staff_schedule_rules")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffScheduleRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "society_id", nullable = false)
    private Long societyId;

    @Column(name = "staff_id", nullable = false)
    private Long staffId;

    @Column(name = "staff_name", length = 150)
    private String staffName;

    @Column(length = 30)
    private String role;

    @Column(name = "allowed_start_time", length = 10)
    private String allowedStartTime;

    @Column(name = "allowed_end_time", length = 10)
    private String allowedEndTime;

    @Column(name = "allowed_days_of_week", columnDefinition = "TEXT")
    private String allowedDaysOfWeek;

    @Column(name = "is_active")
    private Boolean isActive;

    @PrePersist
    protected void onCreate() {
        if (isActive == null) isActive = true;
    }
}

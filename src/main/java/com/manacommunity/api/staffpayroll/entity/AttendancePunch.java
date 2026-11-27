package com.manacommunity.api.staffpayroll.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "staff_attendance_punches")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class AttendancePunch extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public enum PunchType { GATE_IN, GATE_OUT }
    public enum AttendanceStatus { PRESENT, HALF_DAY, ABSENT, PAID_LEAVE, WEEKLY_OFF }

    @Column(nullable = false) private Long communityId;
    @Column(nullable = false) private Long staffId;
    private String staffCode;
    private Long contractId;
    private String employerFlatNumber;

    @Column(nullable = false) private LocalDate punchDate;
    @Enumerated(EnumType.STRING) private PunchType punchType;
    private LocalTime punchTime;

    private String gateId;
    private String verifiedBy;

    @Enumerated(EnumType.STRING) private AttendanceStatus attendanceStatus;
    @Builder.Default private boolean autoMarked = false;

    private long durationMinutes;
}

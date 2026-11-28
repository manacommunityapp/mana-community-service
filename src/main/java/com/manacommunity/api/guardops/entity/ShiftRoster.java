package com.manacommunity.api.guardops.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "shift_rosters")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ShiftRoster extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String communityId;
    private Long guardId;
    private String guardName;
    private String gateId;
    private String gateName;

    @Enumerated(EnumType.STRING)
    private ShiftType shiftType;

    private LocalDate shiftDate;
    private LocalTime scheduledStart;
    private LocalTime scheduledEnd;
    private LocalTime actualClockIn;
    private LocalTime actualClockOut;

    @Enumerated(EnumType.STRING)
    private ClockInMethod clockInMethod;

    private String selfieUrl;
    private boolean biometricVerified;
    private boolean lateArrival;
    private int lateMinutes;

    @Enumerated(EnumType.STRING)
    private ShiftStatus status;

    public enum ShiftType { DAY_SHIFT, NIGHT_SHIFT, SPLIT_SHIFT, EMERGENCY_COVER }
    public enum ClockInMethod { BIOMETRIC, SELFIE_GPS, SUPERVISOR_OVERRIDE, QR_CODE }
    public enum ShiftStatus { SCHEDULED, CLOCKED_IN, ON_DUTY, HANDOVER_PENDING, COMPLETED, NO_SHOW, CANCELLED }
}

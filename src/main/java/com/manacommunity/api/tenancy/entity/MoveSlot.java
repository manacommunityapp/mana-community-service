package com.manacommunity.api.tenancy.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "move_slots")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MoveSlot extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long communityId;
    private String towerName;
    private LocalDate slotDate;
    private LocalTime startTime;
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    private SlotType slotType;

    private String flatNumber;
    private Long tenantRecordId;
    private String serviceElevatorId;
    private boolean gatePassIssued;
    private String gatePassNumber;

    @Enumerated(EnumType.STRING)
    private SlotStatus status;

    private String vehicleNumber;
    private String vehicleType;
    private String driverName;
    private String driverPhone;

    public enum SlotType {
        MOVE_IN, MOVE_OUT
    }

    public enum SlotStatus {
        REQUESTED, APPROVED, GATE_PASS_ISSUED, IN_PROGRESS,
        COMPLETED, CANCELLED, REJECTED
    }
}

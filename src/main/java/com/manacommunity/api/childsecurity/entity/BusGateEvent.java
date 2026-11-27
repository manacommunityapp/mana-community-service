package com.manacommunity.api.childsecurity.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "bus_gate_events")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BusGateEvent extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long busRouteId;
    private String communityId;
    private String rfidTagId;
    private String gateId;

    @Enumerated(EnumType.STRING)
    private GateEventType eventType;

    private LocalDateTime scannedAt;
    private int childrenOnBoard;
    private int childrenDropped;

    private boolean allParentsNotified;
    private LocalDateTime notificationsSentAt;

    private String guardOnDuty;

    public enum GateEventType { RFID_ENTRY, RFID_EXIT, MANUAL_OVERRIDE }
}

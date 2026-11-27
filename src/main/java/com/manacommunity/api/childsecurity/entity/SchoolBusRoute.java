package com.manacommunity.api.childsecurity.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;

@Entity
@Table(name = "school_bus_routes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SchoolBusRoute extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String communityId;
    private String routeCode;
    private String schoolName;
    private String busNumber;
    private String driverName;
    private String driverPhone;
    private String attendantName;
    private String attendantPhone;
    private String rfidTagId;

    private LocalTime morningPickupTime;
    private LocalTime afternoonDropTime;

    private int totalChildren;
    private boolean isActive;

    @Enumerated(EnumType.STRING)
    private BusStatus currentStatus;

    public enum BusStatus { NOT_STARTED, EN_ROUTE_PICKUP, AT_SCHOOL, EN_ROUTE_DROP, AT_GATE, COMPLETED }
}

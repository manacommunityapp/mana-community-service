package com.manacommunity.api.childsecurity.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "bus_boarding_logs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BusBoardingLog extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long busRouteId;
    private Long childId;
    private String childName;
    private String communityId;

    private LocalDate boardingDate;

    @Enumerated(EnumType.STRING)
    private BoardingType boardingType;

    @Enumerated(EnumType.STRING)
    private BoardingStatus boardingStatus;

    private LocalDateTime boardedAt;
    private LocalDateTime droppedAt;
    private String droppedConfirmedBy;

    private boolean parentNotified;
    private LocalDateTime parentNotifiedAt;

    private String remarks;

    public enum BoardingType { MORNING_PICKUP, AFTERNOON_DROP }

    public enum BoardingStatus { BOARDED, DROPPED_SAFE, ABSENT, CANCELLED, PARENT_PICKUP }
}

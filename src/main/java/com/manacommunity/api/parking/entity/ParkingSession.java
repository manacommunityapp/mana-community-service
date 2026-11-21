package com.manacommunity.api.parking.entity;

import com.manacommunity.api.model.Community;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "parking_session",
       indexes = {
           @Index(name = "idx_ps_community", columnList = "community_id,entry_time"),
           @Index(name = "idx_ps_slot",      columnList = "slot_id")
       })
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ParkingSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slot_id")
    private ParkingSlot slot;

    @Column(name = "vehicle_number", nullable = false, length = 30)
    private String vehicleNumber;

    @Column(name = "entry_time", nullable = false)
    @Builder.Default
    private Instant entryTime = Instant.now();

    @Column(name = "exit_time")
    private Instant exitTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "session_type", nullable = false, length = 30)
    @Builder.Default
    private SessionType sessionType = SessionType.VISITOR;

    @Column(name = "authorized_by")
    private Long authorizedBy;

    @Column(name = "overstay_flag", nullable = false)
    @Builder.Default
    private Boolean overstayFlag = false;

    @Column(name = "overstay_notified_at")
    private Instant overstayNotifiedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public enum SessionType { RESIDENT, VISITOR, DELIVERY, STAFF }
}

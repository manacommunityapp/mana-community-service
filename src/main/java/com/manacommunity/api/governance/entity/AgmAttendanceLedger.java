package com.manacommunity.api.governance.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(
    name = "agm_attendance_ledger",
    uniqueConstraints = @UniqueConstraint(name = "uq_agm_attendance_flat", columnNames = {"meeting_id", "flat_id"})
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AgmAttendanceLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "meeting_id", nullable = false)
    private Long meetingId;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(name = "flat_id", nullable = false)
    private Long flatId;

    @Column(name = "attendee_user_id", nullable = false)
    private Long attendeeUserId;

    @Column(name = "proxy_holder_user_id")
    private Long proxyHolderUserId;

    @Column(name = "attendee_role", nullable = false, length = 24)
    @Builder.Default
    private String attendeeRole = "OWNER"; // OWNER, CO_OWNER, PROXY

    @Column(name = "attendance_mode", nullable = false, length = 24)
    @Builder.Default
    private String attendanceMode = "IN_PERSON_QR"; // IN_PERSON_QR, ONLINE_WS

    @Column(nullable = false, length = 24)
    @Builder.Default
    private String status = "CHECKED_IN"; // CHECKED_IN, PROXY_REVOKED, LEFT_MEETING, REGISTERED_PROXY

    @Column(name = "weight_uds", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal weightUds = BigDecimal.ONE;

    @CreationTimestamp
    @Column(name = "checked_in_at", updatable = false)
    private OffsetDateTime checkedInAt;

    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;
}

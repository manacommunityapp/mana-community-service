package com.manacommunity.api.communityops.move;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "move_request")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MoveRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(name = "resident_user_id", nullable = false)
    private Long residentUserId;

    @Column(name = "flat_number", nullable = false)
    private String flatNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "move_type", nullable = false)
    private MoveType moveType;

    @Column(name = "planned_date", nullable = false)
    private LocalDate plannedDate;

    @Column(name = "planned_time")
    private LocalTime plannedTime;

    @Column(name = "vehicle_number")
    private String vehicleNumber;

    @Column(name = "movers_company")
    private String moversCompany;

    @Column(name = "contact_number")
    private String contactNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MoveStatus status = MoveStatus.REQUESTED;

    @Column(name = "reviewed_by")
    private Long reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "review_notes")
    private String reviewNotes;

    @Column(name = "noc_issued", nullable = false)
    private boolean nocIssued = false;

    @Column(name = "noc_issued_at")
    private Instant nocIssuedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}

package com.cpn.domain.mentorship.model;

import com.cpn.domain.common.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "mentor_bookings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MentorBooking extends TenantAwareEntity {

    @Column(nullable = false)
    private UUID mentorId;

    private String mentorName;

    @Column(nullable = false)
    private UUID menteeId;

    private String menteeName;
    private String topic;
    private String dateSlot;
    private String status; // REQUESTED, CONFIRMED, COMPLETED
}

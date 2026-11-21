package com.manacommunity.api.healthcare.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "doctor_slot_utilization")
@Data
public class DoctorSlotUtilization {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "community_id", nullable = false) private Long communityId;
    @Column(name = "doctor_id", nullable = false) private Long doctorId;
    @Column(name = "slot_date", nullable = false) private LocalDate slotDate;
    @Column(name = "total_slots") private Integer totalSlots = 0;
    @Column(name = "booked_slots") private Integer bookedSlots = 0;
    @Column(name = "attended_slots") private Integer attendedSlots = 0;
    @Column(name = "no_show_slots") private Integer noShowSlots = 0;
    @Column(name = "cancelled_slots") private Integer cancelledSlots = 0;
    @Column(name = "calculated_at") private Instant calculatedAt = Instant.now();
}

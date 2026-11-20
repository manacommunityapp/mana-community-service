package com.manacommunity.api.resident.household;

import com.manacommunity.api.resident.model.CommunityFlat;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "household_staff_link", indexes = {
    @Index(name = "idx_staff_link_flat", columnList = "flat_id"),
    @Index(name = "idx_staff_link_active", columnList = "flat_id, active")
})
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class HouseholdStaffLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flat_id", nullable = false)
    private CommunityFlat flat;

    @Column(name = "staff_name", nullable = false, length = 200)
    private String staffName;

    @Column(name = "staff_type", nullable = false, length = 30)
    private String staffType; // MAID, COOK, DRIVER, NANNY, GARDENER, OTHER

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "external_ref_id")
    private Long externalRefId;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "since_date")
    private LocalDate sinceDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}

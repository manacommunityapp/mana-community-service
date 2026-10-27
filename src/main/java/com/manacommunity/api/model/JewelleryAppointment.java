package com.manacommunity.api.model;

import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "jewellery_appointment", indexes = {
    @Index(name = "idx_jappt_user",    columnList = "user_id"),
    @Index(name = "idx_jappt_vendor",  columnList = "vendor_id"),
    @Index(name = "idx_jappt_status",  columnList = "status"),
    @Index(name = "idx_jappt_date",    columnList = "appointment_date")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JewelleryAppointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private JewelleryVendorProfile vendor;

    @Column(name = "appointment_date", nullable = false)
    private LocalDate appointmentDate;

    @Column(name = "appointment_time", nullable = false, length = 20)
    private String appointmentTime;

    /** BROWSE | CUSTOM | BRIDAL | REPAIR | VALUATION */
    @Column(name = "purpose", nullable = false, length = 30)
    private String purpose;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    /** PENDING | CONFIRMED | COMPLETED | CANCELLED */
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

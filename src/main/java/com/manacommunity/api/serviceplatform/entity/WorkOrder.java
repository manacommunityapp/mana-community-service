package com.manacommunity.api.serviceplatform.entity;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.model.Invoice;
import com.manacommunity.api.serviceplatform.entity.enums.WorkOrderStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "work_order", indexes = {
        @Index(name = "idx_work_order_request", columnList = "service_request_id"),
        @Index(name = "idx_work_order_provider", columnList = "provider_id"),
        @Index(name = "idx_work_order_community", columnList = "community_id"),
        @Index(name = "idx_work_order_status", columnList = "status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_request_id", nullable = false, unique = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private ServiceRequest serviceRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private ServiceProvider provider;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Community community;

    @Column(name = "scheduled_start")
    private LocalDateTime scheduledStart;

    @Column(name = "scheduled_end")
    private LocalDateTime scheduledEnd;

    @Column(name = "actual_start")
    private LocalDateTime actualStart;

    @Column(name = "actual_end")
    private LocalDateTime actualEnd;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private WorkOrderStatus status = WorkOrderStatus.CREATED;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "checklist_items", columnDefinition = "jsonb")
    private String checklistItems;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "materials_used", columnDefinition = "jsonb")
    private String materialsUsed;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "before_photos", columnDefinition = "jsonb")
    private String beforePhotos;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "after_photos", columnDefinition = "jsonb")
    private String afterPhotos;

    @Column(name = "resident_signoff")
    @Builder.Default
    private boolean residentSignoff = false;

    @Column(name = "resident_signoff_at")
    private LocalDateTime residentSignoffAt;

    @Column(name = "provider_signoff")
    @Builder.Default
    private boolean providerSignoff = false;

    @Column(name = "provider_signoff_at")
    private LocalDateTime providerSignoffAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Invoice invoice;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
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

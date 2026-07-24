package com.manacommunity.api.vendor.entity;

import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "vms_work_order_photos")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VmsWorkOrderPhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_order_id", nullable = false)
    private VmsWorkOrder workOrder;

    @Column(nullable = false, length = 500)
    private String url;

    @Column(name = "photo_type", nullable = false, length = 20)
    @Builder.Default
    private String photoType = "BEFORE";

    @Column(length = 300)
    private String caption;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by")
    private AppUser uploadedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }
}

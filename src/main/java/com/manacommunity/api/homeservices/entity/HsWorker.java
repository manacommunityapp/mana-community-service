package com.manacommunity.api.homeservices.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity @Table(name = "hs_worker") @Data @Builder @NoArgsConstructor @AllArgsConstructor
public class HsWorker {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "community_id", nullable = false) private Long communityId;
    @Column(nullable = false) private String name;
    private String phone;
    private String email;
    @Column(name = "is_external", nullable = false) private Boolean isExternal = false;
    @Column(precision = 3, scale = 2) private BigDecimal rating;
    @Column(name = "total_jobs", nullable = false) private Integer totalJobs = 0;
    @Column(name = "vms_vendor_id") private Long vmsVendorId;
    @Column(name = "user_id") private Long userId;
    @Column(name = "is_active", nullable = false) private Boolean isActive = true;
    @Column(name = "created_at") private Instant createdAt = Instant.now();
}

package com.manacommunity.api.homeservices.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity @Table(name = "hs_recurring_plan") @Data @Builder @NoArgsConstructor @AllArgsConstructor
public class HsRecurringPlan {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "community_id", nullable = false) private Long communityId;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "package_id") private Long packageId;
    @Column(nullable = false, columnDefinition = "hs_service_category") private String category;
    @Column(nullable = false, columnDefinition = "hs_plan_frequency") private String frequency;
    @Column(name = "next_service_date", nullable = false) private LocalDate nextServiceDate;
    @Column(name = "preferred_time") private LocalTime preferredTime;
    @Column(nullable = false, columnDefinition = "hs_plan_status") private String status = "ACTIVE";
    @Column(name = "created_at") private Instant createdAt = Instant.now();
    @Column(name = "updated_at") private Instant updatedAt = Instant.now();
}

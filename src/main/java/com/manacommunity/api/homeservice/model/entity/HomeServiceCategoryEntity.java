package com.manacommunity.api.homeservice.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "home_service_categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HomeServiceCategoryEntity {
    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 20)
    private String icon;

    @Column(name = "supports_recurring")
    @Builder.Default
    private Boolean supportsRecurring = true;

    @Column(name = "supports_monthly")
    @Builder.Default
    private Boolean supportsMonthly = true;

    @Column(name = "supports_daily")
    @Builder.Default
    private Boolean supportsDaily = true;

    @Column(name = "supports_hourly")
    @Builder.Default
    private Boolean supportsHourly = true;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean active = true;

    @Column(name = "display_order")
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}

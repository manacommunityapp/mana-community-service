package com.cpn.domain.learning.model;

import com.cpn.domain.common.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "courses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Course extends TenantAwareEntity {

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    private String category;
    private String level; // BEGINNER, INTERMEDIATE, ADVANCED
    private Double durationHours;
    private BigDecimal price;
    private boolean isFree;
    private Integer enrollmentCount;
}

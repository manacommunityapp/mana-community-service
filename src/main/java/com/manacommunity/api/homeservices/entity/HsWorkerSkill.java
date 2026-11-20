package com.manacommunity.api.homeservices.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "hs_worker_skill",
    uniqueConstraints = @UniqueConstraint(columnNames = {"worker_id", "category"}))
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class HsWorkerSkill {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "worker_id", nullable = false) private Long workerId;
    @Column(nullable = false, columnDefinition = "hs_service_category") private String category;
    @Column(name = "years_exp") private Integer yearsExp;
    @Column(name = "is_primary") private Boolean isPrimary = false;
}

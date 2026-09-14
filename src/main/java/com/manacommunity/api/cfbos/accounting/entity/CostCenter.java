package com.manacommunity.api.cfbos.accounting.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cfbos_cost_center")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CostCenter extends BaseAuditEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 20)
    private String code;
    @Column(nullable = false, length = 100)
    private String name;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private CostCenter parent;
    @Column(name = "is_active", nullable = false) @Builder.Default
    private Boolean isActive = true;
}

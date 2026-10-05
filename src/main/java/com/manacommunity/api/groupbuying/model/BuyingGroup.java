package com.manacommunity.api.groupbuying.model;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "buying_group")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BuyingGroup extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(nullable = false)
    private String name;

    private String tower;

    private String block;

    @Column(name = "leader_id", nullable = false)
    private Long leaderId;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "total_saved", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalSaved = BigDecimal.ZERO;

    @Column(name = "member_count")
    @Builder.Default
    private Integer memberCount = 1;

    @Column(name = "active_deals_count")
    @Builder.Default
    private Integer activeDealsCount = 0;
}

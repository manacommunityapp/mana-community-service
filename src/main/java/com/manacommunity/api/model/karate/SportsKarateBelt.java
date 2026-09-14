package com.manacommunity.api.model.karate;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.model.SportsMeta;
import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "sports_karate_belt",
        uniqueConstraints = @UniqueConstraint(name = "uq_karate_belt_community_rank",
                columnNames = {"community_id", "rank"}))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class SportsKarateBelt extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id")
    private Community community;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sport_id")
    private SportsMeta sport;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(name = "color_hex", length = 7)
    private String colorHex;

    @Column(nullable = false)
    private Integer rank;

    @Column(name = "min_classes_required", nullable = false)
    @Builder.Default
    private Integer minClassesRequired = 0;

    @Column(name = "min_months_required", nullable = false)
    @Builder.Default
    private Integer minMonthsRequired = 0;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
}

package com.manacommunity.api.helpdesk.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tower_distances", uniqueConstraints = {
    @UniqueConstraint(name = "uq_tower_pair", columnNames = {"community_id", "tower_a", "tower_b"})
})
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class TowerDistance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(name = "tower_a", nullable = false, length = 20)
    private String towerA;

    @Column(name = "tower_b", nullable = false, length = 20)
    private String towerB;

    @Column(name = "walk_minutes", nullable = false)
    private Integer walkMinutes;
}

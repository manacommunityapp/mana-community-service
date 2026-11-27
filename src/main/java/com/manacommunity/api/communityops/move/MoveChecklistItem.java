package com.manacommunity.api.communityops.move;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "move_checklist_item")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MoveChecklistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Enumerated(EnumType.STRING)
    @Column(name = "move_type", nullable = false)
    private MoveType moveType;

    @Column(name = "item_name", nullable = false)
    private String itemName;

    @Builder.Default
    @Column(name = "is_mandatory", nullable = false)
    private boolean mandatory = false;

    @Builder.Default
    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;
}

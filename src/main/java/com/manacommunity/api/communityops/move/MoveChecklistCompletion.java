package com.manacommunity.api.communityops.move;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "move_checklist_completion")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MoveChecklistCompletion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "move_request_id", nullable = false)
    private Long moveRequestId;

    @Column(name = "checklist_item_id", nullable = false)
    private Long checklistItemId;

    @Column(nullable = false)
    private boolean completed = false;

    @Column(name = "completed_by")
    private Long completedBy;

    @Column(name = "completed_at")
    private Instant completedAt;

    private String notes;
}

package com.manacommunity.api.workflow;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Arrays;
import java.util.List;

@Entity
@Table(name = "workflow_transition_rule")
@Getter
@Setter
@NoArgsConstructor
public class WorkflowTransitionRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "workflow_id", nullable = false)
    private String workflowId;

    @Column(name = "from_status", nullable = false)
    private String fromStatus;

    @Column(name = "to_status", nullable = false)
    private String toStatus;

    /** Stored as comma-separated role names, e.g. "RESIDENT,ADMIN,SUPER_ADMIN". */
    @Column(name = "allowed_roles", nullable = false)
    private String allowedRoles;

    @Column(name = "reason_required", nullable = false)
    private boolean reasonRequired;

    @Column(name = "display_label")
    private String displayLabel;

    @Column(name = "sort_order", nullable = false)
    private short sortOrder;

    public List<String> getAllowedRolesList() {
        if (allowedRoles == null || allowedRoles.isBlank()) return List.of();
        return Arrays.asList(allowedRoles.split(","));
    }
}

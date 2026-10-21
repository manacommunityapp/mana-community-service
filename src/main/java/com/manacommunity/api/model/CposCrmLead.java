package com.manacommunity.api.model;

import com.manacommunity.api.model.Community;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cpos_crm_lead")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CposCrmLead {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @Column(name = "lead_name", nullable = false, length = 255)
    private String leadName;

    @Column(length = 255)
    private String email;

    @Column(length = 50)
    private String phone;

    @Column(length = 50, nullable = false)
    @Builder.Default
    private String source = "PORTAL";

    @Column(length = 50, nullable = false)
    @Builder.Default
    private String stage = "NEW";

    @Column(name = "lead_type", length = 50, nullable = false)
    @Builder.Default
    private String leadType = "APARTMENT";

    @Column(name = "budget_lakhs", nullable = false)
    @Builder.Default
    private Integer budgetLakhs = 0;

    @Column(name = "budget_display", length = 100)
    private String budgetDisplay;

    @Column(name = "assigned_to", length = 255)
    private String assignedTo;

    @Column(name = "days_in_stage", nullable = false)
    @Builder.Default
    private Integer daysInStage = 0;

    @Column(name = "days_total", nullable = false)
    @Builder.Default
    private Integer daysTotal = 0;

    @Column(name = "stage_changed_at")
    @Builder.Default
    private LocalDateTime stageChangedAt = LocalDateTime.now();

    @Column(name = "follow_up_at")
    private LocalDateTime followUpAt;

    @Column(name = "follow_up_note", length = 500)
    private String followUpNote;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean deleted = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @OneToMany(mappedBy = "lead", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<CposCrmActivity> activities = new ArrayList<>();

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

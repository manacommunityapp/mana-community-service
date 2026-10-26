package com.manacommunity.api.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cpos_crm_drip_sequence")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CposCrmDripSequence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "trigger_stage", length = 50)
    private String triggerStage;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @OneToMany(mappedBy = "sequence", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<CposCrmDripStep> steps = new ArrayList<>();
}

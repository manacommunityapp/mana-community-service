package com.manacommunity.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "inventory_warehouse", indexes = {
    @Index(name = "idx_inv_warehouse_community", columnList = "community_id"),
})
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"locations", "community"})
public class InventoryWarehouse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    /** Short code used in location paths and picking sequences (e.g. "WH"). */
    @Column(nullable = false, length = 10)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "reception_steps", nullable = false, length = 20)
    @Builder.Default
    private StepType receptionSteps = StepType.ONE_STEP;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_steps", nullable = false, length = 20)
    @Builder.Default
    private StepType deliverySteps = StepType.ONE_STEP;

    @OneToMany(mappedBy = "warehouse", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("completeName ASC")
    @Builder.Default
    @JsonIgnore
    private List<InventoryLocation> locations = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id")
    private Community community;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate  void onUpdate() { updatedAt = LocalDateTime.now(); }

    public enum StepType { ONE_STEP, TWO_STEPS, THREE_STEPS }
}

package com.manacommunity.api.cfbos.charge.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "cfbos_slab_config")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SlabConfig {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 100) private String name;
    private String description;
    @Column(name = "unit_label", length = 30) private String unitLabel;
    @Column(name = "is_active", nullable = false) @Builder.Default
    private Boolean isActive = true;
    @OneToMany(mappedBy = "slabConfig", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TierConfig> tiers;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;
    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }
    @PreUpdate protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}

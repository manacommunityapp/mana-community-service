package com.manacommunity.api.cfbos.charge.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "cfbos_formula")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Formula {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 100) private String name;
    @Column(nullable = false, columnDefinition = "TEXT") private String expression;
    private String description;
    @Column(name = "result_type", nullable = false, length = 20) @Builder.Default
    private String resultType = "AMOUNT";
    @Column(name = "is_active", nullable = false) @Builder.Default
    private Boolean isActive = true;
    @OneToMany(mappedBy = "formula", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FormulaVariable> variables;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;
    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }
    @PreUpdate protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}

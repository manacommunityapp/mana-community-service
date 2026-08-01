package com.manacommunity.api.cfbos.charge.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cfbos_formula_variable")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class FormulaVariable {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "formula_id", nullable = false)
    private Formula formula;
    @Column(name = "variable_name", nullable = false, length = 50) private String variableName;
    @Column(name = "variable_source", nullable = false, length = 50) private String variableSource;
    @Column(name = "source_field", nullable = false, length = 100) private String sourceField;
    @Column(name = "default_value", length = 50) private String defaultValue;
    private String description;
}

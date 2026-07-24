package com.manacommunity.api.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "email_dynamic_field",
        uniqueConstraints = @UniqueConstraint(name = "uk_email_dynamic_field_key", columnNames = "field_key")
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailDynamicField {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String category;

    @Column(name = "field_key", nullable = false, length = 120)
    private String fieldKey;

    @Column(name = "display_name", nullable = false, length = 140)
    private String displayName;

    @Column(name = "sample_value", length = 500)
    private String sampleValue;

    @Column(length = 500)
    private String description;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @PrePersist
    void onCreate() {
        if (sortOrder == null) {
            sortOrder = 0;
        }
    }
}

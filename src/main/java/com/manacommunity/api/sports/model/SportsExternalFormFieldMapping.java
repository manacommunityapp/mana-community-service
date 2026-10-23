package com.manacommunity.api.sports.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "sports_external_form_field_mapping")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SportsExternalFormFieldMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "form_id", nullable = false)
    private SportsExternalForm form;

    @Column(name = "mana_field_key", nullable = false, length = 64)
    private String manaFieldKey;

    @Column(name = "external_question_id", nullable = false, length = 255)
    private String externalQuestionId;

    @Column(name = "question_title", nullable = false, length = 255)
    private String questionTitle;

    @Enumerated(EnumType.STRING)
    @Column(name = "mapping_type", nullable = false, length = 32)
    @Builder.Default
    private MappingType mappingType = MappingType.DIRECT;

    @Column(name = "mapping_rules_json", columnDefinition = "TEXT")
    private String mappingRulesJson;

    @Column(nullable = false)
    @Builder.Default
    private Boolean required = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum MappingType { DIRECT, CATEGORY_LOOKUP, FORMAT_LOOKUP, CUSTOM }
}

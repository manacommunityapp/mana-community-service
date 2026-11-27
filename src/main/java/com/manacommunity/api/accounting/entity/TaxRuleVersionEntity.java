package com.manacommunity.api.accounting.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "tax_rule_versions", schema = "accounting")
@Getter
@Setter
@NoArgsConstructor
public class TaxRuleVersionEntity {

    @Id
    @Column(name = "rule_version_id", nullable = false)
    private UUID ruleVersionId;

    @Column(name = "rule_family", nullable = false, length = 30)
    private String ruleFamily;

    @Column(name = "fy_code", nullable = false, length = 10)
    private String fyCode;

    @Column(name = "version_no", nullable = false)
    private int versionNo;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "published_at", nullable = false)
    private Instant publishedAt = Instant.now();

    @Column(name = "gazette_ref", length = 200)
    private String gazetteRef;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "rule_spec", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> ruleSpec;

    @Column(name = "spec_sha256", nullable = false, length = 64)
    private String specSha256;
}

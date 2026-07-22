package com.manacommunity.api.email.builder.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "custom_email_template")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomEmailTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "subject", nullable = false)
    private String subject;

    @Column(name = "html", columnDefinition = "TEXT")
    private String html;

    @Column(name = "css", columnDefinition = "TEXT")
    private String css;

    @Column(name = "layout_json", columnDefinition = "TEXT")
    private String layoutJson;

    @Column(name = "generated_css", columnDefinition = "TEXT")
    private String generatedCss;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private TemplateStatus status = TemplateStatus.DRAFT;

    @Column(name = "category", length = 50)
    private String category;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tags", columnDefinition = "jsonb")
    private List<String> tags;

    @Column(name = "module_key", length = 50)
    private String moduleKey;

    @Column(name = "menu_key", length = 50)
    private String menuKey;

    @Column(name = "menu_label", length = 100)
    private String menuLabel;

    @Column(name = "sub_menu_key", length = 50)
    private String subMenuKey;

    @Column(name = "sub_menu_label", length = 100)
    private String subMenuLabel;

    @Column(name = "use_case", length = 100)
    private String useCase;

    @Column(name = "trigger_key", length = 100)
    private String triggerKey;

    @Column(name = "template_key", length = 100)
    private String templateKey;

    @Column(name = "theme_name", length = 100)
    private String themeName;

    @Column(name = "theme_json", columnDefinition = "TEXT")
    private String themeJson;

    @Column(name = "version", nullable = false)
    @Builder.Default
    private Integer version = 1;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

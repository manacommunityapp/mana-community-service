package com.manacommunity.api.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "email_template",
        uniqueConstraints = @UniqueConstraint(name = "uk_email_template_community_code", columnNames = {"community_id", "template_code"})
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailTemplateDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @Column(name = "template_code", nullable = false, length = 80)
    private String templateCode;

    @Column(name = "template_name", nullable = false, length = 160)
    private String templateName;

    @Column(length = 80)
    private String category;

    @Column(nullable = false, length = 240)
    private String subject;

    @Column(name = "html_content", nullable = false, columnDefinition = "TEXT")
    private String htmlContent;

    @Column(name = "layout_json", columnDefinition = "TEXT")
    private String layoutJson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "header_id")
    private EmailHeader header;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "footer_id")
    private EmailFooter footer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EmailTemplateStatus status;

    @Column(nullable = false)
    private Integer version;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (version == null) {
            version = 1;
        }
        if (status == null) {
            status = EmailTemplateStatus.DRAFT;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

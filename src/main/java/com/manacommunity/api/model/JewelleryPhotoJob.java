package com.manacommunity.api.model;

import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "jewellery_photo_job", indexes = {
    @Index(name = "idx_jpj_user",    columnList = "user_id"),
    @Index(name = "idx_jpj_status",  columnList = "status, created_at"),
    @Index(name = "idx_jpj_expires", columnList = "expires_at")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JewelleryPhotoJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    /** QUEUED | PROCESSING | COMPLETED | FAILED | EXPIRED | DELETION_PENDING */
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private String status = "QUEUED";

    /** S3 object key for the source image — never a public URL */
    @Column(name = "source_object_key", length = 500)
    private String sourceObjectKey;

    /** S3 object key for the composed output image */
    @Column(name = "output_object_key", length = 500)
    private String outputObjectKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private JewelleryProduct product;

    /** MIRRAR_PHOTO | BANUBA_PHOTO | COMPOSITION_V1 */
    @Column(name = "provider", length = 40)
    private String provider;

    /** Whether the user explicitly saved this output for longer retention */
    @Column(name = "user_saved", nullable = false)
    @Builder.Default
    private Boolean userSaved = false;

    /** When the job and its assets should be purged */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "error_code", length = 60)
    private String errorCode;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (expiresAt == null) {
            expiresAt = createdAt.plusHours(24);
        }
    }
}

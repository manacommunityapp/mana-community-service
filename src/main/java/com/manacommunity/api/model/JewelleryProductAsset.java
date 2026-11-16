package com.manacommunity.api.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "jewellery_product_asset", indexes = {
    @Index(name = "idx_jpa_product", columnList = "product_id, asset_type")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JewelleryProductAsset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private JewelleryProduct product;

    /** IMAGE_MAIN | IMAGE_GALLERY | AR_2D | AR_3D */
    @Column(name = "asset_type", nullable = false, length = 40)
    private String assetType;

    @Column(name = "asset_url", nullable = false, length = 500)
    private String assetUrl;

    /** EAR | NECK | HAND | WRIST | FOREHEAD */
    @Column(name = "body_part", length = 30)
    private String bodyPart;

    @Column(name = "format", length = 20)
    private String format;

    @Column(name = "asset_version", nullable = false)
    @Builder.Default
    private Integer assetVersion = 1;

    /** PENDING | APPROVED | REJECTED */
    @Column(name = "approval_status", nullable = false, length = 20)
    @Builder.Default
    private String approvalStatus = "PENDING";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "placement_metadata", columnDefinition = "jsonb")
    private Map<String, Object> placementMetadata;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

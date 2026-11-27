package com.manacommunity.api.access.biometric;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "biometric_edge_deltas")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BiometricEdgeDelta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(name = "sync_version", nullable = false)
    private Long syncVersion;

    @Column(nullable = false, length = 16)
    private String op; // UPSERT, REVOKE

    @Column(name = "enrollment_id")
    private Long enrollmentId;

    @Column(name = "person_id", nullable = false)
    private Long personId;

    @Column(name = "person_name", length = 120)
    private String personName;

    @Column(name = "face_embedding_hash", nullable = false, length = 255)
    private String faceEmbeddingHash;

    @Column(name = "embedding_vector_b64", columnDefinition = "TEXT")
    private String embeddingVectorB64;

    @Column(length = 255)
    private String reason;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}

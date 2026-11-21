package com.manacommunity.platform.ops;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "platform_deploy_events")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PlatformDeployEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "service_name", nullable = false, length = 100)
    private String serviceName;

    @Column(nullable = false, length = 100)
    private String version;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String environment = "PRODUCTION";

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "deployed_by", length = 200)
    private String deployedBy;

    @Column(name = "commit_sha", length = 50)
    private String commitSha;

    @Column(name = "release_notes", columnDefinition = "text")
    private String releaseNotes;

    @Column(name = "started_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant startedAt = Instant.now();

    @Column(name = "finished_at")
    private Instant finishedAt;
}

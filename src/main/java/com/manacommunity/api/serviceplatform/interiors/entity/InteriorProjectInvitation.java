package com.manacommunity.api.serviceplatform.interiors.entity;

import com.manacommunity.api.serviceplatform.entity.ServiceProvider;
import com.manacommunity.api.serviceplatform.entity.ServiceRequest;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "interior_project_invitation", uniqueConstraints = {
        @UniqueConstraint(name = "uq_interior_invitation_project_provider", columnNames = {"service_request_id", "provider_id"})
}, indexes = {
        @Index(name = "idx_interior_invitation_provider_status", columnList = "provider_id,status"),
        @Index(name = "idx_interior_invitation_request", columnList = "service_request_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InteriorProjectInvitation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "service_request_id", nullable = false)
    private ServiceRequest serviceRequest;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "provider_id", nullable = false)
    private ServiceProvider provider;

    @Column(nullable = false, length = 20)
    @Builder.Default private String status = "INVITED";

    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}

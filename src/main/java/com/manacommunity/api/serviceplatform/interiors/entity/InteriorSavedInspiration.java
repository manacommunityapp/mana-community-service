package com.manacommunity.api.serviceplatform.interiors.entity;

import com.manacommunity.api.serviceplatform.entity.ServiceRequest;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "interior_saved_inspiration", uniqueConstraints = @UniqueConstraint(name = "uq_interior_inspiration_user_url", columnNames = {"user_id", "source_url"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InteriorSavedInspiration {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "user_id", nullable = false) private Long userId;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "service_request_id") private ServiceRequest serviceRequest;
    @Column(name = "source_url", nullable = false, columnDefinition = "TEXT") private String sourceUrl;
    @Column(name = "image_url", columnDefinition = "TEXT") private String imageUrl;
    @Column(length = 240) private String title;
    @Column(columnDefinition = "TEXT") private String notes;
    @Column(columnDefinition = "TEXT") private String tags;
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;
    @PrePersist void onCreate(){ createdAt=LocalDateTime.now(); updatedAt=createdAt; }
    @PreUpdate void onUpdate(){ updatedAt=LocalDateTime.now(); }
}

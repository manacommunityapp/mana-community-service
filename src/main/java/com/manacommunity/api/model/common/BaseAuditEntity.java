package com.manacommunity.api.model.common;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Shared audit columns for all entity tables that need full actor + timestamp tracking.
 *
 * Subclasses get createdAt/updatedAt (via @PrePersist/@PreUpdate) and
 * createdBy/updatedBy (via Spring Data @CreatedBy/@LastModifiedBy resolved
 * from SecurityAuditorAware).  Add @EntityListeners is inherited from here —
 * subclasses must NOT add it again.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public abstract class BaseAuditEntity {

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @CreatedBy
    @Column(name = "created_by", nullable = false, updatable = false)
    private Long createdBy;

    @LastModifiedBy
    @Column(name = "updated_by", nullable = false)
    private Long updatedBy;

    @PrePersist
    protected void initAudit() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void touchAudit() {
        updatedAt = LocalDateTime.now();
    }
}

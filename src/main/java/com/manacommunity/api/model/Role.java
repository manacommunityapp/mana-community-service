package com.manacommunity.api.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.hibernate.envers.RelationTargetAuditMode;

import java.util.HashSet;
import java.util.Set;

/**
 * RolePermission Entity
 * Maps user roles (e.g. ADMIN, MEMBER, VENDOR) to dynamic menu-based permission keys.
 */
@Entity
@Audited
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"permissions", "community"})
@Table(name = "roles", uniqueConstraints = @UniqueConstraint(columnNames = {"name", "community_id"}))
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false)
    private String name; // e.g., "SUPER_ADMIN"

    @Column(name = "community_id")
    private Long communityId;

    /**
     * Read-only navigation to the owning community (null for global roles).
     * Writes still go through {@link #communityId}; the DB FK on community_id is
     * managed by SchemaConstraintPatcher, so Hibernate must not generate its own.
     */
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", insertable = false, updatable = false,
                foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Community community;

    // The core mapping linked directly by role_id to role_permissions.role_id
    @NotAudited
    @OneToMany(mappedBy = "roleEntity", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private Set<RolePermission> permissions = new HashSet<>();
}

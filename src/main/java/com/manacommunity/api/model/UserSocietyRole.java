package com.manacommunity.api.model;

import com.manacommunity.api.model.common.BaseAuditEntity;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_society_role", uniqueConstraints = {
    @UniqueConstraint(name = "uq_user_society_role",
                      columnNames = {"user_id", "community_id", "role_type"})
})
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSocietyRole extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    @ToString.Exclude
    private Community community;

    @Column(name = "role_type", nullable = false, length = 40)
    private String roleType;

    @Column(name = "flat_no", length = 20)
    private String flatNo;

    @Column(name = "is_primary", nullable = false)
    @Builder.Default
    private Boolean isPrimary = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(length = 255)
    private String notes;
}

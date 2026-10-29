package com.manacommunity.api.broadcast.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import lombok.*;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "broadcasts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Broadcast extends BaseAuditEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id")
    private Community community;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private AppUser createdByUser;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Builder.Default
    private String category = "GENERAL";

    @Builder.Default
    private String priority = "NORMAL";

    @Builder.Default
    private String target = "ALL";

    @Builder.Default
    private String status = "SENT";

    private LocalDateTime scheduledAt;
}

package com.manacommunity.api.watchlist.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import lombok.*;
import jakarta.persistence.*;

@Entity
@Table(name = "watchlist_entries")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class WatchlistEntry extends BaseAuditEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id")
    private Community community;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "added_by_user_id")
    private AppUser addedBy;

    private String name;
    private String phone;
    private String vehicleNumber;
    private String photo;
    private String reason;
    private String category;

    @Builder.Default
    private String status = "ACTIVE";

    private String notes;
}

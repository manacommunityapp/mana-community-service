package com.manacommunity.api.cctv.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import com.manacommunity.api.model.Community;
import lombok.*;
import jakarta.persistence.*;

@Entity
@Table(name = "cameras")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Camera extends BaseAuditEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id")
    private Community community;

    private String name;
    private String location;
    private String streamUrl;
    private String snapshotUrl;

    @Builder.Default
    private String status = "ONLINE";

    @Builder.Default
    private Boolean isNightMode = false;
}

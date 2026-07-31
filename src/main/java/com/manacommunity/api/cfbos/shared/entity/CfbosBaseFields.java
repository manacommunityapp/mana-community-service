package com.manacommunity.api.cfbos.shared.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Embeddable
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CfbosBaseFields {
    @Column(name = "is_deleted")
    @Builder.Default
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "version")
    @Builder.Default
    private Integer version = 0;
}

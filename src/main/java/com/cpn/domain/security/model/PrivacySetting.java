package com.cpn.domain.security.model;

import com.cpn.domain.common.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "privacy_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrivacySetting extends TenantAwareEntity {

    @Column(nullable = false, unique = true)
    private UUID userId;

    private String profileVisibility; // PUBLIC, COMMUNITY_ONLY, PRIVATE
    private boolean showFlatNumber;
    private boolean showPhoneNumber;
    private boolean allowDirectMessages;
    private boolean dataExportRequested;
}

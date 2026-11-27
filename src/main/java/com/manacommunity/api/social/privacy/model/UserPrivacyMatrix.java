package com.manacommunity.api.social.privacy.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Granular visibility matrix configured by a resident.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPrivacyMatrix {
    private Long userId;
    private Long communityId;

    @Builder.Default
    private VisibilityScope phoneScope = VisibilityScope.VERIFIED_OWNERS_ONLY;

    @Builder.Default
    private VisibilityScope vehicleScope = VisibilityScope.EVERYONE_IN_SOCIETY;

    @Builder.Default
    private VisibilityScope flatScope = VisibilityScope.EVERYONE_IN_SOCIETY;

    @Builder.Default
    private VisibilityScope emailScope = VisibilityScope.HIDDEN_COMPLETELY;
}

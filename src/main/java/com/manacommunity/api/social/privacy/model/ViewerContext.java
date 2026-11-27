package com.manacommunity.api.social.privacy.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.Set;

/**
 * Context of the resident performing a directory search or browsing members.
 * Resolved once per incoming HTTP request at the security layer.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ViewerContext {
    private Long viewerUserId;
    private Long communityId;
    private boolean isVerifiedOwner;
    private boolean isSocietyAdmin;

    @Builder.Default
    private Set<Long> joinedClubIds = Collections.emptySet();
}

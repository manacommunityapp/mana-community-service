package com.manacommunity.api.social.privacy.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.Set;

/**
 * Raw internal directory entry containing sensitive attributes.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResidentDirectoryEntry {
    private Long userId;
    private Long communityId;
    private String residentName;
    private String flatNumber;
    private String phoneNumber;
    private String vehicleNumber;
    private String email;

    @Builder.Default
    private Set<Long> joinedClubIds = Collections.emptySet();
}

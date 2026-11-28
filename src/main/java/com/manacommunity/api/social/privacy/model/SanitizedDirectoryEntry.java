package com.manacommunity.api.social.privacy.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Clean user-facing directory entry with masked or omitted fields based on privacy matrix.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SanitizedDirectoryEntry {
    private Long userId;
    private String residentName;
    private String flatNumber;
    private String phoneNumber;
    private String vehicleNumber;
    private String email;

    // Metadata flags for the client UI
    private boolean phoneMasked;
    private boolean vehicleMasked;
    private boolean flatMasked;
    private boolean emailMasked;
}

package com.manacommunity.api.events.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BulkDonationUploadResult {
    private int totalRows;
    private int savedCount;
    private int failedCount;
}

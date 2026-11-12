package com.manacommunity.api.kyc.dto;

import com.manacommunity.api.kyc.entity.KycDocument;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KycSummaryResponse {
    private String overallStatus;
    private List<KycDocument> documents;
}

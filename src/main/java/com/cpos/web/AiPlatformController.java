package com.cpos.web;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/cpos/ai")
@RequiredArgsConstructor
@CrossOrigin
public class AiPlatformController {

    @GetMapping("/valuation/{propertyId}")
    public ResponseEntity<Map<String, Object>> getAiValuation(@PathVariable UUID propertyId) {
        Map<String, Object> val = new HashMap<>();
        val.put("propertyId", propertyId);
        val.put("predictedMarketValue", 15600000);
        val.put("confidenceScorePct", 94.0);
        val.put("predicted1YrAppreciationPct", 7.8);
        val.put("expectedRentalYieldPct", 6.2);
        val.put("aiInvestmentScore", 87);
        val.put("recommendation", "STRONG_BUY");
        return ResponseEntity.ok(val);
    }
}

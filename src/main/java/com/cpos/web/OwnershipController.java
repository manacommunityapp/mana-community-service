package com.cpos.web;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@org.springframework.context.annotation.Profile("!prod") // hardcoded stub -- no real ownership service yet
@org.springframework.security.access.prepost.PreAuthorize("hasRole('SUPER_ADMIN')")
@RestController
@RequestMapping("/api/v1/cpos/ownership")
@RequiredArgsConstructor
public class OwnershipController {

    @GetMapping("/property/{propertyId}")
    public ResponseEntity<Map<String, Object>> getPropertyOwnership(@PathVariable UUID propertyId) {
        Map<String, Object> res = new HashMap<>();
        res.put("propertyId", propertyId);
        res.put("primaryOwner", "Rajesh Sharma");
        res.put("ownershipType", "INDIVIDUAL");
        res.put("sharePct", 100.0);
        res.put("purchaseDate", "2021-03-15");
        res.put("purchasePrice", 12500000);
        return ResponseEntity.ok(res);
    }
}

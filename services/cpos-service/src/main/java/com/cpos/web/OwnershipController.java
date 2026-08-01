package com.cpos.web;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/cpos/ownership")
@RequiredArgsConstructor
@CrossOrigin
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

package com.manacommunity.api.address.normalization.controller;

import com.manacommunity.api.address.normalization.model.AddressNormalizationRequest;
import com.manacommunity.api.address.normalization.model.AddressNormalizationResponse;
import com.manacommunity.api.address.normalization.service.AddressEntityResolutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller exposing unstructured address normalization and entity resolution APIs.
 */
@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
public class AddressResolutionController {

    private final AddressEntityResolutionService resolutionService;

    @PostMapping("/normalize-and-resolve")
    public ResponseEntity<AddressNormalizationResponse> normalizeAndResolve(
            @RequestBody AddressNormalizationRequest request) {
        AddressNormalizationResponse response = resolutionService.resolveAddress(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/parse")
    public ResponseEntity<AddressNormalizationResponse> parseAddressQuery(
            @RequestParam String rawAddress,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String pincode) {
        AddressNormalizationRequest request = new AddressNormalizationRequest(rawAddress, city, pincode);
        AddressNormalizationResponse response = resolutionService.resolveAddress(request);
        return ResponseEntity.ok(response);
    }
}

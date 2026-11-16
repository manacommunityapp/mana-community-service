package com.manacommunity.api.parcel.controller;

import com.manacommunity.api.parcel.entity.ParcelRecord;
import com.manacommunity.api.parcel.service.ParcelService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/parcels")
@RequiredArgsConstructor
public class ParcelController {

    private final ParcelService parcelService;
    private final LoggedInUserService loggedInUserService;

    @PostMapping
    public ResponseEntity<ParcelRecord> receiveParcel(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody ReceiveParcelRequest request) {
        AppUser caller = loggedInUserService.resolve(principal);
        ParcelRecord parcel = parcelService.receiveParcel(
                caller.getCommunity().getId(),
                request.getFlatNo(),
                request.getCourierName(),
                request.getTrackingNumber(),
                caller.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(parcel);
    }

    @GetMapping
    public ResponseEntity<List<ParcelRecord>> getParcels(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String flatNo) {
        AppUser caller = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(parcelService.getPendingParcels(caller.getCommunity().getId(), flatNo));
    }

    @PostMapping("/{id}/generate-otp")
    public ResponseEntity<ParcelRecord> generateOtp(@PathVariable Long id) {
        return ResponseEntity.ok(parcelService.generateCollectionOtp(id));
    }

    @PostMapping("/{id}/collect")
    public ResponseEntity<ParcelRecord> collectParcel(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String otp = body.get("otp");
        return ResponseEntity.ok(parcelService.collectParcel(id, otp));
    }

    @Data
    public static class ReceiveParcelRequest {
        private String flatNo;
        private String courierName;
        private String trackingNumber;
    }
}

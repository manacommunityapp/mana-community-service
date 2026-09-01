package com.manacommunity.api.events.controller;

import com.manacommunity.api.events.dto.EventDonationRequest;
import com.manacommunity.api.events.dto.EventDonationResponse;
import com.manacommunity.api.events.service.EventDonationService;
import com.manacommunity.api.events.service.EventDonationService.BulkUploadOutcome;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/events/donations")
@RequiredArgsConstructor
public class EventDonationController {

    private final EventDonationService donationService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping
    @PreAuthorize("hasAuthority('View Events')")
    public ResponseEntity<List<EventDonationResponse>> getAll(
            @RequestParam(required = false) Long eventId,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (eventId != null) {
            return ResponseEntity.ok(donationService.getByEvent(eventId));
        }
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        if (communityId == null) return ResponseEntity.ok(List.of());
        return ResponseEntity.ok(donationService.getByCommunity(communityId));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('Create Event')")
    public ResponseEntity<EventDonationResponse> create(
            @Valid @RequestBody EventDonationRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(donationService.create(req, user, user.getCommunity()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('Create Event')")
    public ResponseEntity<EventDonationResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody EventDonationRequest req) {
        return ResponseEntity.ok(donationService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('Create Event')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        donationService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/bulk-upload/template")
    @PreAuthorize("hasAuthority('Create Event')")
    public ResponseEntity<byte[]> downloadTemplate() throws IOException {
        byte[] bytes = donationService.generateUploadTemplate();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("donation_upload_template.xlsx").build().toString())
                .body(bytes);
    }

    @PostMapping(value = "/bulk-upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('Create Event')")
    public ResponseEntity<byte[]> bulkUpload(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal) throws IOException {
        AppUser user = loggedInUserService.resolve(principal);
        BulkUploadOutcome outcome = donationService.bulkUpload(file, user, user.getCommunity());

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Total-Rows",  String.valueOf(outcome.total()));
        headers.set("X-Saved-Count", String.valueOf(outcome.saved()));
        headers.set("X-Failed-Count", String.valueOf(outcome.failed()));
        headers.setAccessControlExposeHeaders(List.of("X-Total-Rows", "X-Saved-Count", "X-Failed-Count"));
        headers.setContentDisposition(
                ContentDisposition.attachment().filename("donation_upload_result.xlsx").build());
        headers.setContentType(
                MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));

        return ResponseEntity.ok().headers(headers).body(outcome.resultExcel());
    }
}

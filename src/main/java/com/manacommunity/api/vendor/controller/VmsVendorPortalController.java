package com.manacommunity.api.vendor.controller;

import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import com.manacommunity.api.vendor.service.VmsVendorPortalService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vendor")
@RequiredArgsConstructor
public class VmsVendorPortalController {
    private final VmsVendorPortalService portalService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping("/availability/mine")
    @PreAuthorize("isAuthenticated()")
    public Map<String, Object> getMyAvailability(@AuthenticationPrincipal UserPrincipal principal) {
        return portalService.getMyAvailability(user(principal));
    }

    @GetMapping("/vendors/{vendorId}/availability")
    @PreAuthorize("hasAuthority('View Vendor Management')")
    public Map<String, Object> getAvailability(@PathVariable Long vendorId, @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = user(principal);
        return portalService.getAvailability(vendorId, user.getCommunity().getId());
    }

    @GetMapping("/vendors/{vendorId}/slots")
    @PreAuthorize("hasAuthority('View Vendor Management')")
    public List<String> getAvailableSlots(@PathVariable Long vendorId, @RequestParam Long serviceId,
                                          @RequestParam LocalDate date, @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = user(principal);
        return portalService.getAvailableSlots(vendorId, serviceId, date, user.getCommunity().getId());
    }

    @PutMapping("/availability/hours")
    @PreAuthorize("isAuthenticated()")
    public Map<String, Object> updateHours(@RequestBody Map<String, Object> hours, @AuthenticationPrincipal UserPrincipal principal) {
        return portalService.updateOperatingHours(user(principal), hours);
    }

    @PostMapping("/availability/holidays")
    @PreAuthorize("isAuthenticated()")
    public Map<String, Object> addHoliday(@RequestBody Map<String, String> body, @AuthenticationPrincipal UserPrincipal principal) {
        return portalService.addHoliday(user(principal), body);
    }

    @DeleteMapping("/availability/holidays/{holidayId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> removeHoliday(@PathVariable Long holidayId, @AuthenticationPrincipal UserPrincipal principal) {
        portalService.removeHoliday(user(principal), holidayId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/availability/vacation")
    @PreAuthorize("isAuthenticated()")
    public Map<String, Object> setVacation(@RequestBody Map<String, Object> body, @AuthenticationPrincipal UserPrincipal principal) {
        return portalService.setVacationMode(user(principal), Boolean.TRUE.equals(body.get("enabled")),
                (String) body.get("start"), (String) body.get("end"));
    }

    @GetMapping("/favorites")
    @PreAuthorize("isAuthenticated()")
    public List<Map<String, Object>> favorites(@AuthenticationPrincipal UserPrincipal principal) {
        return portalService.getFavorites(user(principal));
    }

    @PostMapping("/favorites/{vendorId}")
    @PreAuthorize("isAuthenticated()")
    public Map<String, Object> addFavorite(@PathVariable Long vendorId, @AuthenticationPrincipal UserPrincipal principal) {
        return portalService.addFavorite(user(principal), vendorId);
    }

    @DeleteMapping("/favorites/{vendorId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> removeFavorite(@PathVariable Long vendorId, @AuthenticationPrincipal UserPrincipal principal) {
        portalService.removeFavorite(user(principal), vendorId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/favorites/{vendorId}/check")
    @PreAuthorize("isAuthenticated()")
    public Map<String, Object> checkFavorite(@PathVariable Long vendorId, @AuthenticationPrincipal UserPrincipal principal) {
        return portalService.checkFavorite(user(principal), vendorId);
    }

    @GetMapping("/documents/mine")
    @PreAuthorize("isAuthenticated()")
    public List<Map<String, Object>> myDocuments(@AuthenticationPrincipal UserPrincipal principal) {
        return portalService.getMyDocuments(user(principal));
    }

    @GetMapping("/vendors/{vendorId}/documents")
    @PreAuthorize("hasAuthority('View Vendor Management')")
    public List<Map<String, Object>> documents(@PathVariable Long vendorId, @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = user(principal);
        return portalService.getDocuments(vendorId, user.getCommunity().getId());
    }

    @PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public Map<String, Object> uploadDocument(@RequestParam("file") MultipartFile file,
            @RequestParam String documentType, @RequestParam String documentName,
            @RequestParam(required = false) String expiryDate, @AuthenticationPrincipal UserPrincipal principal) {
        return portalService.uploadDocument(user(principal), file, documentType, documentName, expiryDate);
    }

    @DeleteMapping("/documents/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteDocument(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        portalService.deleteDocument(user(principal), id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/documents/{id}/verify")
    @PreAuthorize("hasAuthority('Manage Vendors')")
    public Map<String, Object> verifyDocument(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        return portalService.verifyDocument(user(principal), id);
    }

    @GetMapping("/documents/{id}/file")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Resource> downloadDocument(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) throws Exception {
        AppUser user = user(principal);
        boolean manager = principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("Manage Vendors"));
        Path path = portalService.documentPath(id, user.getCommunity().getId(), user.getId(), manager);
        String type = Files.probeContentType(path);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(type == null ? "application/octet-stream" : type))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(path.getFileName().toString()).build().toString())
                .body(new FileSystemResource(path));
    }

    private AppUser user(UserPrincipal principal) { return loggedInUserService.resolve(principal); }
}

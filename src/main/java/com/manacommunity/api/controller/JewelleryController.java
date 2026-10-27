package com.manacommunity.api.controller;

import com.manacommunity.api.dto.JewelleryDto;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.service.JewelleryService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.service.LoggedInUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/jewellery")
@RequiredArgsConstructor
public class JewelleryController {

    private final JewelleryService jewelleryService;
    private final LoggedInUserService loggedInUserService;

    // ── Vendors ───────────────────────────────────────────────────

    @GetMapping("/vendors/nearby")
    public ResponseEntity<List<JewelleryDto.VendorResponse>> getNearbyVendors(
            @RequestParam(required = false) String area) {
        return ResponseEntity.ok(jewelleryService.getNearbyVendors(area));
    }

    @GetMapping("/vendors/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<JewelleryDto.VendorResponse> getMyVendorProfile(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(jewelleryService.getMyVendorProfile(user));
    }

    @PostMapping("/vendors/register")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<JewelleryDto.VendorResponse> registerVendor(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody JewelleryDto.RegisterVendorRequest req) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(jewelleryService.registerVendor(user, req));
    }

    /** Public storefront — used by the buyer vendor/[vendorId] screen. */
    @GetMapping("/vendors/{id}")
    public ResponseEntity<JewelleryDto.VendorResponse> getVendorById(
            @PathVariable Long id) {
        return ResponseEntity.ok(jewelleryService.getVendorById(id));
    }

    /** Merchant-facing: fetch appointments booked for the logged-in vendor. */
    @GetMapping("/vendor/appointments")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<JewelleryDto.AppointmentResponse>> getVendorAppointments(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(jewelleryService.getVendorAppointments(user));
    }

    /** Merchant-facing: accept, confirm or mark complete an appointment. */
    @PatchMapping("/vendor/appointments/{id}/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> updateAppointmentStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        AppUser user = loggedInUserService.resolve(principal);
        jewelleryService.updateAppointmentStatus(user, id, body.get("status"));
        return ResponseEntity.noContent().build();
    }

    // ── Products ──────────────────────────────────────────────────

    @GetMapping("/products")
    public ResponseEntity<Page<JewelleryDto.ProductResponse>> getProducts(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String material,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = principal != null ? loggedInUserService.resolve(principal) : null;
        return ResponseEntity.ok(jewelleryService.getProducts(category, material, q, pageable, user));
    }

    @GetMapping("/products/featured")
    public ResponseEntity<List<JewelleryDto.ProductResponse>> getFeaturedProducts(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = principal != null ? loggedInUserService.resolve(principal) : null;
        return ResponseEntity.ok(jewelleryService.getFeaturedProducts(user));
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<JewelleryDto.ProductResponse> getProduct(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = principal != null ? loggedInUserService.resolve(principal) : null;
        return ResponseEntity.ok(jewelleryService.getProduct(id, user));
    }

    @GetMapping("/vendors/{vendorId}/products")
    public ResponseEntity<Page<JewelleryDto.ProductResponse>> getVendorProducts(
            @PathVariable Long vendorId,
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = principal != null ? loggedInUserService.resolve(principal) : null;
        return ResponseEntity.ok(jewelleryService.getVendorProducts(vendorId, pageable, user));
    }

    @PostMapping("/products")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<JewelleryDto.ProductResponse> createProduct(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody JewelleryDto.CreateProductRequest req) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(jewelleryService.createProduct(user, req));
    }

    // ── Try-On ────────────────────────────────────────────────────

    @PostMapping("/try-on/sessions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<JewelleryDto.TryOnSessionResponse> startTryOnSession(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody JewelleryDto.StartTryOnRequest req) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(jewelleryService.startTryOnSession(user, req));
    }

    @PostMapping("/try-on/sessions/{sessionId}/photo-jobs")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<JewelleryDto.PhotoJobResponse> submitPhotoJob(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long sessionId,
            @RequestBody Map<String, String> body) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(jewelleryService.submitPhotoJob(user, sessionId, body.get("sourceS3Key")));
    }

    @GetMapping("/try-on/photo-jobs/{jobId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<JewelleryDto.PhotoJobResponse> getPhotoJobStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long jobId) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(jewelleryService.getPhotoJobStatus(user, jobId));
    }

    @DeleteMapping("/try-on/photo-jobs/{jobId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deletePhoto(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long jobId) {
        AppUser user = loggedInUserService.resolve(principal);
        jewelleryService.deletePhoto(user, jobId);
        return ResponseEntity.noContent().build();
    }

    // ── Wishlist ──────────────────────────────────────────────────

    @GetMapping("/wishlist")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<JewelleryDto.WishlistItemResponse>> getWishlist(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(jewelleryService.getWishlist(user));
    }

    @PostMapping("/wishlist")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<JewelleryDto.WishlistItemResponse> addToWishlist(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody JewelleryDto.AddToWishlistRequest req) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(jewelleryService.addToWishlist(user, req.getProductId()));
    }

    @DeleteMapping("/wishlist/{productId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> removeFromWishlist(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long productId) {
        AppUser user = loggedInUserService.resolve(principal);
        jewelleryService.removeFromWishlist(user, productId);
        return ResponseEntity.noContent().build();
    }

    // ── Appointments ──────────────────────────────────────────────

    @GetMapping("/appointments")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<JewelleryDto.AppointmentResponse>> getMyAppointments(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(jewelleryService.getMyAppointments(user));
    }

    @PostMapping("/appointments")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<JewelleryDto.AppointmentResponse> bookAppointment(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody JewelleryDto.CreateAppointmentRequest req) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(jewelleryService.bookAppointment(user, req));
    }

    @PatchMapping("/appointments/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> cancelAppointment(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        AppUser user = loggedInUserService.resolve(principal);
        String reason = body != null ? body.get("reason") : null;
        jewelleryService.cancelAppointment(user, id, reason);
        return ResponseEntity.noContent().build();
    }
}

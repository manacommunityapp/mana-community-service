package com.manacommunity.api.service;

import com.manacommunity.api.dto.JewelleryDto;
import com.manacommunity.api.user.model.AppUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface JewelleryService {

    // Vendor
    JewelleryDto.VendorResponse registerVendor(AppUser user, JewelleryDto.RegisterVendorRequest req);
    JewelleryDto.VendorResponse getMyVendorProfile(AppUser user);
    List<JewelleryDto.VendorResponse> getNearbyVendors(String area);
    /** Public storefront: single vendor by id. */
    JewelleryDto.VendorResponse getVendorById(Long id);
    /** Merchant-facing: appointments booked with the caller's vendor profile. */
    List<JewelleryDto.AppointmentResponse> getVendorAppointments(AppUser user);
    /** Merchant-facing: update appointment status (CONFIRMED / COMPLETED / CANCELLED). */
    void updateAppointmentStatus(AppUser user, Long appointmentId, String status);

    // Products
    Page<JewelleryDto.ProductResponse> getProducts(String category, String material, String q, Pageable pageable, AppUser user);
    List<JewelleryDto.ProductResponse> getFeaturedProducts(AppUser user);
    JewelleryDto.ProductResponse getProduct(Long id, AppUser user);
    Page<JewelleryDto.ProductResponse> getVendorProducts(Long vendorId, Pageable pageable, AppUser user);
    JewelleryDto.ProductResponse createProduct(AppUser user, JewelleryDto.CreateProductRequest req);

    // Try-On
    JewelleryDto.TryOnSessionResponse startTryOnSession(AppUser user, JewelleryDto.StartTryOnRequest req);
    JewelleryDto.PhotoJobResponse submitPhotoJob(AppUser user, Long sessionId, String sourceS3Key);
    JewelleryDto.PhotoJobResponse getPhotoJobStatus(AppUser user, Long jobId);
    void deletePhoto(AppUser user, Long jobId);

    // Wishlist
    List<JewelleryDto.WishlistItemResponse> getWishlist(AppUser user);
    JewelleryDto.WishlistItemResponse addToWishlist(AppUser user, Long productId);
    void removeFromWishlist(AppUser user, Long productId);

    // Appointments
    List<JewelleryDto.AppointmentResponse> getMyAppointments(AppUser user);
    JewelleryDto.AppointmentResponse bookAppointment(AppUser user, JewelleryDto.CreateAppointmentRequest req);
    void cancelAppointment(AppUser user, Long appointmentId, String reason);
}

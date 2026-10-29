package com.manacommunity.api.service.impl;

import com.manacommunity.api.dto.JewelleryDto;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.model.*;
import com.manacommunity.api.repository.*;
import com.manacommunity.api.service.JewelleryService;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.manacommunity.api.media.service.MediaUrlService;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class JewelleryServiceImpl implements JewelleryService {

    private final JewelleryVendorProfileRepository vendorRepo;
    private final JewelleryProductRepository productRepo;
    private final JewelleryProductAssetRepository assetRepo;
    private final JewelleryTryOnSessionRepository tryOnRepo;
    private final JewelleryPhotoJobRepository photoJobRepo;
    private final JewelleryWishlistRepository wishlistRepo;
    private final JewelleryAppointmentRepository appointmentRepo;
    private final MediaUrlService mediaUrlService;

    private static final DateTimeFormatter DT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    // ── Vendor ────────────────────────────────────────────────────

    @Override
    public JewelleryDto.VendorResponse registerVendor(AppUser user, JewelleryDto.RegisterVendorRequest req) {
        if (vendorRepo.existsByUser(user)) {
            throw new IllegalStateException("Vendor profile already exists for this user");
        }
        JewelleryVendorProfile vendor = JewelleryVendorProfile.builder()
                .user(user)
                .shopName(req.getShopName())
                .ownerName(req.getOwnerName())
                .phoneNumber(req.getPhoneNumber())
                .email(req.getEmail())
                .address(req.getAddress())
                .serviceAreas(req.getServiceAreas())
                .specialties(req.getSpecialties())
                .gstNumber(req.getGstNumber())
                .panNumber(req.getPanNumber())
                .bisHallmarkLicense(req.getBisHallmarkLicense())
                .build();
        return toVendorResponse(vendorRepo.save(vendor));
    }

    @Override
    @Transactional(readOnly = true)
    public JewelleryDto.VendorResponse getMyVendorProfile(AppUser user) {
        return vendorRepo.findByUser(user)
                .map(this::toVendorResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor profile not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<JewelleryDto.VendorResponse> getNearbyVendors(String area) {
        return vendorRepo.findByIsActiveTrueOrderByRatingDesc()
                .stream()
                .map(this::toVendorResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public JewelleryDto.VendorResponse getVendorById(Long id) {
        return vendorRepo.findByIdAndIsActiveTrue(id)
                .map(this::toVendorResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<JewelleryDto.AppointmentResponse> getVendorAppointments(AppUser user) {
        JewelleryVendorProfile vendor = vendorRepo.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor profile not found"));
        return appointmentRepo.findByVendorOrderByAppointmentDateDescCreatedAtDesc(vendor)
                .stream()
                .map(this::toAppointmentResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void updateAppointmentStatus(AppUser user, Long appointmentId, String status) {
        JewelleryVendorProfile vendor = vendorRepo.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor profile not found"));
        JewelleryAppointment appt = appointmentRepo.findByIdAndVendor(appointmentId, vendor)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("Status must not be blank");
        }
        appt.setStatus(status.toUpperCase());
        appointmentRepo.save(appt);
    }

    // ── Products ──────────────────────────────────────────────────


    @Override
    @Transactional(readOnly = true)
    public Page<JewelleryDto.ProductResponse> getProducts(
            String category, String material, String q, Pageable pageable, AppUser user) {
        List<JewelleryProduct> list;
        if (q != null && !q.isBlank()) {
            list = productRepo.searchActive(q, pageable);
        } else if (category != null && material != null) {
            list = productRepo.findByCategoryAndMaterialAndStatusOrderByCreatedAtDesc(category, material, "ACTIVE");
        } else if (category != null) {
            list = productRepo.findByCategoryAndStatusOrderByCreatedAtDesc(category, "ACTIVE");
        } else if (material != null) {
            list = productRepo.findByMaterialAndStatusOrderByCreatedAtDesc(material, "ACTIVE");
        } else {
            // Use Pageable-aware method for the unfiltered case so pagination info is correct
            return productRepo.findByStatusOrderByCreatedAtDesc("ACTIVE", pageable)
                    .map(p -> toProductResponse(p, user));
        }
        // Manually build a Page from the filtered list for consistent pagination response
        int start = (int) pageable.getOffset();
        int end   = Math.min(start + pageable.getPageSize(), list.size());
        List<JewelleryProduct> pageSlice = start >= list.size() ? List.of() : list.subList(start, end);
        return new org.springframework.data.domain.PageImpl<>(
                pageSlice.stream().map(p -> toProductResponse(p, user)).collect(Collectors.toList()),
                pageable,
                list.size());
    }

    @Override
    @Transactional(readOnly = true)
    public List<JewelleryDto.ProductResponse> getFeaturedProducts(AppUser user) {
        return productRepo.findFeatured(Pageable.ofSize(10))
                .stream()
                .map(p -> toProductResponse(p, user))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public JewelleryDto.ProductResponse getProduct(Long id, AppUser user) {
        JewelleryProduct product = productRepo.findByIdAndStatus(id, "ACTIVE")
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        return toProductResponse(product, user);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<JewelleryDto.ProductResponse> getVendorProducts(Long vendorId, Pageable pageable, AppUser user) {
        JewelleryVendorProfile vendor = vendorRepo.findByIdAndIsActiveTrue(vendorId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found"));
        List<JewelleryProduct> list = productRepo.findByVendorAndStatusOrderByCreatedAtDesc(vendor, "ACTIVE");
        int start = (int) pageable.getOffset();
        int end   = Math.min(start + pageable.getPageSize(), list.size());
        List<JewelleryProduct> pageSlice = start >= list.size() ? List.of() : list.subList(start, end);
        return new org.springframework.data.domain.PageImpl<>(
                pageSlice.stream().map(p -> toProductResponse(p, user)).collect(Collectors.toList()),
                pageable,
                list.size());
    }

    @Override
    public JewelleryDto.ProductResponse createProduct(AppUser user, JewelleryDto.CreateProductRequest req) {
        JewelleryVendorProfile vendor = vendorRepo.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor profile not found. Please register as a vendor first."));
        JewelleryProduct product = JewelleryProduct.builder()
                .vendor(vendor)
                .name(req.getName())
                .category(req.getCategory())
                .material(req.getMaterial())
                .purity(req.getPurity())
                .netWeightG(req.getNetWeightG())
                .description(req.getDescription())
                .huid(req.getHuid())
                .tags(req.getTags())
                .goldRatePerGram(req.getGoldRatePerGram())
                .makingCharges(req.getMakingCharges())
                .stoneCost(req.getStoneCost())
                .totalPrice(req.getTotalPrice())
                .gstPercent(req.getGstPercent())
                .stockQuantity(req.getStockQuantity())
                .stoneDetails(req.getStoneDetails())
                .build();
        return toProductResponse(productRepo.save(product), user);
    }

    // ── Try-On ────────────────────────────────────────────────────

    @Override
    public JewelleryDto.TryOnSessionResponse startTryOnSession(AppUser user, JewelleryDto.StartTryOnRequest req) {
        JewelleryProduct product = productRepo.findById(req.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        JewelleryTryOnSession session = JewelleryTryOnSession.builder()
                .user(user)
                .product(product)
                .mode(req.getMode())
                .platform(req.getPlatform())
                .provider("PHOTO".equals(req.getMode()) ? "INTERNAL" : "MIRRAR")
                .build();
        session = tryOnRepo.save(session);
        return JewelleryDto.TryOnSessionResponse.builder()
                .id(session.getId())
                .productId(product.getId())
                .provider(session.getProvider())
                .mode(session.getMode())
                .platform(session.getPlatform())
                .status(session.getStatus())
                .startedAt(session.getStartedAt().format(DT))
                .build();
    }

    @Override
    public JewelleryDto.PhotoJobResponse submitPhotoJob(AppUser user, Long sessionId, String sourceS3Key) {
        JewelleryTryOnSession session = tryOnRepo.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Try-on session not found"));
        JewelleryPhotoJob job = JewelleryPhotoJob.builder()
                .user(user)
                .product(session.getProduct())
                .provider("INTERNAL")
                .sourceObjectKey(sourceS3Key)
                .build();
        job = photoJobRepo.save(job);
        return toPhotoJobResponse(job);
    }

    @Override
    @Transactional(readOnly = true)
    public JewelleryDto.PhotoJobResponse getPhotoJobStatus(AppUser user, Long jobId) {
        JewelleryPhotoJob job = photoJobRepo.findByIdAndUser(jobId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Photo job not found"));
        return toPhotoJobResponse(job);
    }

    @Override
    public void deletePhoto(AppUser user, Long jobId) {
        JewelleryPhotoJob job = photoJobRepo.findByIdAndUser(jobId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Photo job not found"));
        job.setStatus("DELETION_PENDING");
        photoJobRepo.save(job);
    }

    // ── Wishlist ──────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<JewelleryDto.WishlistItemResponse> getWishlist(AppUser user) {
        return wishlistRepo.findByUserOrderBySavedAtDesc(user)
                .stream()
                .map(w -> JewelleryDto.WishlistItemResponse.builder()
                        .id(w.getId())
                        .productId(w.getProduct().getId())
                        .product(toProductResponse(w.getProduct(), user))
                        .savedAt(w.getSavedAt().format(DT))
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public JewelleryDto.WishlistItemResponse addToWishlist(AppUser user, Long productId) {
        JewelleryProduct product = productRepo.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        if (wishlistRepo.existsByUserAndProduct(user, product)) {
            throw new IllegalStateException("Product already in wishlist");
        }
        JewelleryWishlist item = JewelleryWishlist.builder()
                .user(user)
                .product(product)
                .build();
        item = wishlistRepo.save(item);
        return JewelleryDto.WishlistItemResponse.builder()
                .id(item.getId())
                .productId(productId)
                .product(toProductResponse(product, user))
                .savedAt(item.getSavedAt().format(DT))
                .build();
    }

    @Override
    public void removeFromWishlist(AppUser user, Long productId) {
        JewelleryProduct product = productRepo.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        wishlistRepo.deleteByUserAndProduct(user, product);
    }

    // ── Appointments ──────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<JewelleryDto.AppointmentResponse> getMyAppointments(AppUser user) {
        return appointmentRepo.findByUserOrderByAppointmentDateDescCreatedAtDesc(user)
                .stream()
                .map(this::toAppointmentResponse)
                .collect(Collectors.toList());
    }

    @Override
    public JewelleryDto.AppointmentResponse bookAppointment(AppUser user, JewelleryDto.CreateAppointmentRequest req) {
        JewelleryVendorProfile vendor = vendorRepo.findByIdAndIsActiveTrue(req.getVendorId())
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found"));
        JewelleryAppointment appt = JewelleryAppointment.builder()
                .user(user)
                .vendor(vendor)
                .appointmentDate(req.getDate())
                .appointmentTime(req.getTime())
                .purpose(req.getPurpose())
                .notes(req.getNotes())
                .build();
        return toAppointmentResponse(appointmentRepo.save(appt));
    }

    @Override
    public void cancelAppointment(AppUser user, Long appointmentId, String reason) {
        JewelleryAppointment appt = appointmentRepo.findByIdAndUser(appointmentId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));
        appt.setStatus("CANCELLED");
        appt.setCancellationReason(reason);
        appointmentRepo.save(appt);
    }

    private static final List<String> ALL_SLOTS = List.of(
            "10:00 AM", "11:00 AM", "12:00 PM",
            "02:00 PM", "03:00 PM", "04:00 PM", "05:00 PM"
    );

    @Override
    @Transactional(readOnly = true)
    public List<String> getAvailableSlots(Long vendorId, java.time.LocalDate date) {
        JewelleryVendorProfile vendor = vendorRepo.findByIdAndIsActiveTrue(vendorId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found"));

        List<JewelleryAppointment> booked = appointmentRepo.findByVendorAndAppointmentDateAndStatusIn(
                vendor, date, List.of("PENDING", "CONFIRMED"));

        java.util.Set<String> bookedTimes = booked.stream()
                .map(JewelleryAppointment::getAppointmentTime)
                .map(String::trim)
                .collect(Collectors.toSet());

        return ALL_SLOTS.stream()
                .filter(slot -> !bookedTimes.contains(slot))
                .collect(Collectors.toList());
    }

    // ── Mappers ───────────────────────────────────────────────────

    private JewelleryDto.VendorResponse toVendorResponse(JewelleryVendorProfile v) {
        return JewelleryDto.VendorResponse.builder()
                .id(v.getId())
                .shopName(v.getShopName())
                .ownerName(v.getOwnerName())
                .phoneNumber(v.getPhoneNumber())
                .address(v.getAddress())
                .serviceAreas(v.getServiceAreas())
                .specialties(v.getSpecialties())
                .imageUrl(v.getImageUrl())
                .rating(v.getRating())
                .reviewCount(v.getReviewCount())
                .openHours(v.getOpenHours())
                .isVerified("VERIFIED".equals(v.getVerificationStatus()))
                .build();
    }

    private JewelleryDto.ProductResponse toProductResponse(JewelleryProduct p, AppUser user) {
        List<String> imageUrls = assetRepo
                .findByProductAndAssetTypeAndApprovalStatus(p, "IMAGE_MAIN", "APPROVED")
                .stream()
                .map(JewelleryProductAsset::getAssetUrl)
                .collect(Collectors.toList());

        String tryOnAssetUrl = assetRepo
                .findByProductAndAssetTypeAndApprovalStatus(p, "AR_2D", "APPROVED")
                .stream()
                .findFirst()
                .map(JewelleryProductAsset::getAssetUrl)
                .orElse(null);

        boolean wishlisted = user != null && wishlistRepo.existsByUserAndProduct(user, p);

        return JewelleryDto.ProductResponse.builder()
                .id(p.getId())
                .vendorId(p.getVendor().getId())
                .vendorName(p.getVendor().getShopName())
                .sku(p.getSku())
                .name(p.getName())
                .category(p.getCategory())
                .material(p.getMaterial())
                .purity(p.getPurity())
                .netWeightG(p.getNetWeightG())
                .description(p.getDescription())
                .huid(p.getHuid())
                .imageUrls(imageUrls)
                .tryOnAssetUrl(tryOnAssetUrl)
                .goldRatePerGram(p.getGoldRatePerGram())
                .makingCharges(p.getMakingCharges())
                .stoneCost(p.getStoneCost())
                .totalPrice(p.getTotalPrice())
                .gstPercent(p.getGstPercent())
                .stockQuantity(p.getStockQuantity())
                .status(p.getStatus())
                .isWishlisted(wishlisted)
                .stoneDetails(p.getStoneDetails())
                .build();
    }

    private JewelleryDto.PhotoJobResponse toPhotoJobResponse(JewelleryPhotoJob j) {
        String outputUrl = null;
        if (j.getOutputObjectKey() != null && !j.getOutputObjectKey().isBlank()) {
            outputUrl = j.getOutputObjectKey().startsWith("http")
                    ? j.getOutputObjectKey()
                    : mediaUrlService.resolveStoredUrl(j.getOutputObjectKey());
        }
        return JewelleryDto.PhotoJobResponse.builder()
                .id(j.getId())
                .status(j.getStatus())
                .outputUrl(outputUrl)
                .createdAt(j.getCreatedAt().format(DT))
                .completedAt(j.getCompletedAt() != null ? j.getCompletedAt().format(DT) : null)
                .errorCode(j.getErrorCode())
                .build();
    }

    private JewelleryDto.AppointmentResponse toAppointmentResponse(JewelleryAppointment a) {
        return JewelleryDto.AppointmentResponse.builder()
                .id(a.getId())
                .vendorId(a.getVendor().getId())
                .vendorName(a.getVendor().getShopName())
                .shopAddress(a.getVendor().getAddress())
                .date(a.getAppointmentDate().toString())
                .time(a.getAppointmentTime())
                .purpose(a.getPurpose())
                .notes(a.getNotes())
                .status(a.getStatus())
                .build();
    }
}

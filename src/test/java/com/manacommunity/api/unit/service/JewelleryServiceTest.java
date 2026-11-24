package com.manacommunity.api.unit.service;

import com.manacommunity.api.dto.JewelleryDto;
import com.manacommunity.api.media.service.MediaUrlService;
import com.manacommunity.api.model.*;
import com.manacommunity.api.repository.*;
import com.manacommunity.api.service.impl.JewelleryServiceImpl;
import com.manacommunity.api.user.model.AppUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JewelleryService Unit Tests")
class JewelleryServiceTest {

    @Mock private JewelleryVendorProfileRepository vendorRepo;
    @Mock private JewelleryProductRepository productRepo;
    @Mock private JewelleryProductAssetRepository assetRepo;
    @Mock private JewelleryTryOnSessionRepository tryOnRepo;
    @Mock private JewelleryPhotoJobRepository photoJobRepo;
    @Mock private JewelleryWishlistRepository wishlistRepo;
    @Mock private JewelleryAppointmentRepository appointmentRepo;
    @Mock private MediaUrlService mediaUrlService;

    @InjectMocks
    private JewelleryServiceImpl jewelleryService;

    private AppUser user;
    private JewelleryVendorProfile vendor;
    private JewelleryProduct product;

    @BeforeEach
    void setUp() {
        user = new AppUser();
        user.setId(10L);
        user.setEmail("resident@mana.com");

        vendor = JewelleryVendorProfile.builder()
                .id(5L)
                .user(user)
                .shopName("Mana Gold Palace")
                .ownerName("Aarav Sharma")
                .phoneNumber("9876543210")
                .address("MG Road")
                .specialties("Bridal, Gold, Diamond")
                .verificationStatus("VERIFIED")
                .isActive(true)
                .build();

        product = JewelleryProduct.builder()
                .id(101L)
                .vendor(vendor)
                .sku("JWL-5-101")
                .name("22K Bridal Temple Necklace")
                .category("NECKLACE")
                .material("GOLD")
                .purity("22K (916)")
                .tags("bridal, temple, wedding")
                .totalPrice(new BigDecimal("145000"))
                .stockQuantity(3)
                .status("ACTIVE")
                .build();
    }

    @Test
    @DisplayName("createProduct defaults status to ACTIVE, generates SKU, and saves IMAGE_MAIN & AR_2D assets")
    void createProduct_savesActiveProductAndAssets() {
        when(vendorRepo.findByUser(user)).thenReturn(Optional.of(vendor));
        when(productRepo.save(any(JewelleryProduct.class))).thenAnswer(inv -> {
            JewelleryProduct p = inv.getArgument(0);
            p.setId(202L);
            return p;
        });
        when(assetRepo.findByProductAndAssetTypeAndApprovalStatus(any(), eq("IMAGE_MAIN"), eq("APPROVED")))
                .thenReturn(List.of(JewelleryProductAsset.builder().assetUrl("https://cdn.mana.com/main.jpg").build()));
        when(assetRepo.findByProductAndAssetTypeAndApprovalStatus(any(), eq("AR_2D"), eq("APPROVED")))
                .thenReturn(List.of(JewelleryProductAsset.builder().assetUrl("https://cdn.mana.com/ar.png").build()));

        JewelleryDto.CreateProductRequest req = new JewelleryDto.CreateProductRequest();
        req.setName("Emerald Drop Earrings");
        req.setCategory("earrings");
        req.setMaterial("gold");
        req.setTags("bridal, festive");
        req.setImageUrls(List.of("https://cdn.mana.com/main.jpg"));
        req.setTryOnAssetUrl("https://cdn.mana.com/ar.png");
        req.setTotalPrice(new BigDecimal("52000"));
        req.setStockQuantity(4);

        JewelleryDto.ProductResponse res = jewelleryService.createProduct(user, req);

        assertThat(res.getId()).isEqualTo(202L);
        assertThat(res.getStatus()).isEqualTo("ACTIVE");
        assertThat(res.getSku()).startsWith("JWL-5-");
        assertThat(res.getCategory()).isEqualTo("EARRINGS");
        assertThat(res.getMaterial()).isEqualTo("GOLD");
        assertThat(res.getImageUrls()).containsExactly("https://cdn.mana.com/main.jpg");
        assertThat(res.getTryOnAssetUrl()).isEqualTo("https://cdn.mana.com/ar.png");
        assertThat(res.getTags()).containsExactly("bridal", "festive");
        verify(assetRepo, times(2)).save(any(JewelleryProductAsset.class));
    }

    @Test
    @DisplayName("getProducts filters by category AND search query together and paginates accurately")
    void getProducts_combinedCategoryAndSearch() {
        JewelleryProduct nonMatching = JewelleryProduct.builder()
                .id(102L)
                .vendor(vendor)
                .sku("JWL-5-102")
                .name("Daily Wear Chain")
                .category("NECKLACE")
                .material("GOLD")
                .tags("daily")
                .totalPrice(new BigDecimal("35000"))
                .stockQuantity(5)
                .status("ACTIVE")
                .build();

        when(productRepo.findByCategoryAndStatusOrderByCreatedAtDesc("NECKLACE", "ACTIVE"))
                .thenReturn(List.of(product, nonMatching));

        Page<JewelleryDto.ProductResponse> result = jewelleryService.getProducts(
                "NECKLACE", null, "bridal", PageRequest.of(0, 10), user);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("22K Bridal Temple Necklace");
    }

    @Test
    @DisplayName("addToWishlist is idempotent when product is already wishlisted")
    void addToWishlist_idempotentWhenAlreadySaved() {
        JewelleryWishlist existing = JewelleryWishlist.builder()
                .id(88L)
                .user(user)
                .product(product)
                .savedAt(LocalDateTime.now())
                .build();

        when(productRepo.findById(101L)).thenReturn(Optional.of(product));
        when(wishlistRepo.findByUserAndProduct(user, product)).thenReturn(Optional.of(existing));

        JewelleryDto.WishlistItemResponse res = jewelleryService.addToWishlist(user, 101L);

        assertThat(res.getId()).isEqualTo(88L);
        assertThat(res.getProductId()).isEqualTo(101L);
        verify(wishlistRepo, never()).save(any());
    }

    @Test
    @DisplayName("bookAppointment rejects past dates and already-booked time slots")
    void bookAppointment_preventsPastDateAndDoubleBooking() {
        JewelleryDto.CreateAppointmentRequest pastReq = new JewelleryDto.CreateAppointmentRequest(
                5L, LocalDate.now().minusDays(1), "10:00 AM", "BROWSE", null);

        assertThatThrownBy(() -> jewelleryService.bookAppointment(user, pastReq))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("past");

        LocalDate tomorrow = LocalDate.now().plusDays(1);
        when(vendorRepo.findByIdAndIsActiveTrue(5L)).thenReturn(Optional.of(vendor));
        when(appointmentRepo.findByVendorAndAppointmentDateAndStatusIn(eq(vendor), eq(tomorrow), any()))
                .thenReturn(List.of(JewelleryAppointment.builder()
                        .id(1L)
                        .vendor(vendor)
                        .appointmentDate(tomorrow)
                        .appointmentTime("11:00 AM")
                        .status("CONFIRMED")
                        .build()));

        JewelleryDto.CreateAppointmentRequest duplicateSlotReq = new JewelleryDto.CreateAppointmentRequest(
                5L, tomorrow, "11:00 AM", "BRIDAL", "Looking for bridal set");

        assertThatThrownBy(() -> jewelleryService.bookAppointment(user, duplicateSlotReq))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already booked");
    }

    @Test
    @DisplayName("verifyVendor activates vendor profile and sets verificationStatus to VERIFIED")
    void verifyVendor_activatesVendor() {
        JewelleryVendorProfile pendingVendor = JewelleryVendorProfile.builder()
                .id(9L)
                .user(user)
                .shopName("New Jewellers")
                .ownerName("Kiran")
                .phoneNumber("9876543210")
                .address("Main St")
                .verificationStatus("PENDING")
                .isActive(false)
                .build();

        when(vendorRepo.findById(9L)).thenReturn(Optional.of(pendingVendor));
        when(vendorRepo.save(any(JewelleryVendorProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        JewelleryDto.VendorResponse res = jewelleryService.verifyVendor(9L, true);

        assertThat(res.getIsVerified()).isTrue();
        assertThat(pendingVendor.getIsActive()).isTrue();
        assertThat(pendingVendor.getVerificationStatus()).isEqualTo("VERIFIED");
    }

    @Test
    @DisplayName("submitPhotoJob rejects blank s3Key and prevents cross-user session IDOR")
    void submitPhotoJob_validatesKeyAndSessionOwnership() {
        assertThatThrownBy(() -> jewelleryService.submitPhotoJob(user, 1L, "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sourceS3Key");

        AppUser otherUser = new AppUser();
        otherUser.setId(999L);
        JewelleryTryOnSession otherSession = JewelleryTryOnSession.builder()
                .id(50L)
                .user(otherUser)
                .product(product)
                .mode("PHOTO")
                .platform("ios")
                .provider("INTERNAL")
                .build();

        when(tryOnRepo.findById(50L)).thenReturn(Optional.of(otherSession));

        assertThatThrownBy(() -> jewelleryService.submitPhotoJob(user, 50L, "tryon/source-1.jpg"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Not authorized");
    }

    @Test
    @DisplayName("getVendorAppointments includes customerId, customerName, and customerPhone for merchant view")
    void getVendorAppointments_includesCustomerDetails() {
        user.setFullName("Ananya Rao");
        user.setPhone("9876501234");
        JewelleryAppointment appt = JewelleryAppointment.builder()
                .id(77L)
                .user(user)
                .vendor(vendor)
                .appointmentDate(LocalDate.now().plusDays(2))
                .appointmentTime("03:00 PM")
                .purpose("BRIDAL")
                .notes("Looking for temple bridal set")
                .status("PENDING")
                .build();

        when(vendorRepo.findByUser(user)).thenReturn(Optional.of(vendor));
        when(appointmentRepo.findByVendorOrderByAppointmentDateDescCreatedAtDesc(vendor))
                .thenReturn(List.of(appt));

        List<JewelleryDto.AppointmentResponse> list = jewelleryService.getVendorAppointments(user);

        assertThat(list).hasSize(1);
        assertThat(list.get(0).getCustomerId()).isEqualTo(10L);
        assertThat(list.get(0).getCustomerName()).isEqualTo("Ananya Rao");
        assertThat(list.get(0).getCustomerPhone()).isEqualTo("9876501234");
    }

    @Test
    @DisplayName("RecommendationService.rank returns rule-matched items first and falls back gracefully when fit attributes are absent")
    void recommendationRank_scoresAttributesAndFallsBackWhenEmpty() {
        var candidates = List.of(
                new com.manacommunity.api.fitprofile.ProductCatalogPort.Candidate(1L, "Classic Jhumka", new BigDecimal("45000")),
                new com.manacommunity.api.fitprofile.ProductCatalogPort.Candidate(2L, "Long Chandbali", new BigDecimal("65000"))
        );
        var rules = List.of(
                new com.manacommunity.api.fitprofile.RecommendationService.Rule(
                        "drop_length_mm", new BigDecimal("30"), new BigDecimal("70"), 1.0)
        );

        // Case 1: No product_fit_attributes rows in DB -> fallback returns candidates with 0.5 baseline score
        var fallback = com.manacommunity.api.fitprofile.RecommendationService.rank(
                candidates, rules, java.util.Map.of(), 6);
        assertThat(fallback).hasSize(2);
        assertThat(fallback.get(0).score()).isEqualTo(0.5);
        assertThat(fallback.get(0).reason()).isEqualTo("Curated for your proportions");

        // Case 2: Product 2 has drop_length_mm = 45 (in range 30..70) -> scores 1.0
        var withAttrs = com.manacommunity.api.fitprofile.RecommendationService.rank(
                candidates,
                rules,
                java.util.Map.of(2L, java.util.Map.of("drop_length_mm", new BigDecimal("45"))),
                6);
        assertThat(withAttrs).hasSize(1);
        assertThat(withAttrs.get(0).productId()).isEqualTo(2L);
        assertThat(withAttrs.get(0).score()).isEqualTo(1.0);
        assertThat(withAttrs.get(0).reason()).isEqualTo("Suits your proportions");
    }
}

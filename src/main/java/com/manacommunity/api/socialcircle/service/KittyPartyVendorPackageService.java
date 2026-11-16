package com.manacommunity.api.socialcircle.service;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.repository.CommunityRepository;
import com.manacommunity.api.socialcircle.dto.KittyPartyVendorPackageDtos.*;
import com.manacommunity.api.socialcircle.entity.KittyPartyCircle;
import com.manacommunity.api.socialcircle.entity.KittyPartyVendorPackage;
import com.manacommunity.api.socialcircle.repository.KittyPartyCircleRepository;
import com.manacommunity.api.socialcircle.repository.KittyPartyVendorPackageRepository;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KittyPartyVendorPackageService {

    private final KittyPartyVendorPackageRepository packageRepository;
    private final KittyPartyCircleRepository circleRepository;
    private final CommunityRepository communityRepository;

    @Transactional
    public List<VendorPackageResponse> getPackagesForCircle(Long circleId, String category) {
        KittyPartyCircle circle = circleRepository.findByIdAndDeletedAtIsNull(circleId)
                .orElseThrow(() -> new IllegalArgumentException("Circle not found with id: " + circleId));

        Long communityId = circle.getCommunity().getId();
        ensureDefaultPackagesSeeded(circle.getCommunity());

        List<KittyPartyVendorPackage> packages;
        if (category != null && !category.isBlank() && !"ALL".equalsIgnoreCase(category)) {
            packages = packageRepository.findByCommunityIdAndCategoryAndIsActiveTrueOrderByRatingDesc(communityId, category.toUpperCase());
        } else {
            packages = packageRepository.findByCommunityIdAndIsActiveTrueOrderByRatingDesc(communityId);
        }

        return packages.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public VendorPackageResponse createPackage(Long communityId, CreateVendorPackageRequest request) {
        Community community = communityRepository.findById(communityId)
                .orElseThrow(() -> new IllegalArgumentException("Community not found with id: " + communityId));

        KittyPartyVendorPackage pkg = KittyPartyVendorPackage.builder()
                .community(community)
                .category(request.getCategory().toUpperCase())
                .packageName(request.getPackageName().trim())
                .description(request.getDescription())
                .priceRange(request.getPriceRange())
                .contactPerson(request.getContactPerson())
                .contactPhone(request.getContactPhone())
                .sampleMenu(request.getSampleMenu())
                .rating(request.getRating() != null ? request.getRating() : BigDecimal.valueOf(4.80))
                .isActive(true)
                .build();

        KittyPartyVendorPackage saved = packageRepository.save(pkg);
        log.info("Created party vendor package id={} communityId={}", saved.getId(), communityId);
        return toResponse(saved);
    }

    @Transactional
    public Map<String, Object> inquirePackage(AppUser user, InquireVendorPackageRequest request) {
        KittyPartyVendorPackage pkg = packageRepository.findById(request.getPackageId())
                .orElseThrow(() -> new IllegalArgumentException("Package not found with id: " + request.getPackageId()));

        log.info("Vendor inquiry received for package id='{}' from resident id='{}' phone='{}'",
                pkg.getId(), user.getId(), user.getPhone());

        return Map.of(
                "success", true,
                "message", "Inquiry sent to " + pkg.getContactPerson() + " (" + pkg.getPackageName() + "). They will connect shortly.",
                "contactPhone", pkg.getContactPhone() != null ? pkg.getContactPhone() : "",
                "contactPerson", pkg.getContactPerson() != null ? pkg.getContactPerson() : ""
        );
    }

    private void ensureDefaultPackagesSeeded(Community community) {
        if (packageRepository.existsByCommunityId(community.getId())) {
            return;
        }

        List<KittyPartyVendorPackage> defaults = List.of(
                KittyPartyVendorPackage.builder()
                        .community(community)
                        .category("CATERING")
                        .packageName("Royal Chaat & High Tea Spread")
                        .description("Artisan street chaats, paneer tikka skewers, mini samosas, masala chai & filter coffee station.")
                        .priceRange("₹450 - ₹650 / guest")
                        .contactPerson("Chef Rajesh Gourmet")
                        .contactPhone("+91 98765 43210")
                        .sampleMenu("Pani Puri Shots, Dahi Bhalla, Paneer Tikka, Mini Samosa, Gulab Jamun, Kulhad Chai")
                        .rating(BigDecimal.valueOf(4.90))
                        .isActive(true)
                        .build(),

                KittyPartyVendorPackage.builder()
                        .community(community)
                        .category("CAKES_DESSERTS")
                        .packageName("Celebration Theme Cakes & Dessert Table")
                        .description("Custom fondant or buttercream themed cakes, macarons, cupcakes, and dessert shooters.")
                        .priceRange("₹1,200 - ₹2,500 fixed")
                        .contactPerson("Sugar & Spice Studio")
                        .contactPhone("+91 98765 11223")
                        .sampleMenu("Custom 1.5kg Red Velvet Cake, 12 Belgian Truffle Cupcakes, 12 Mango Panna Cotta Shooters")
                        .rating(BigDecimal.valueOf(4.85))
                        .isActive(true)
                        .build(),

                KittyPartyVendorPackage.builder()
                        .community(community)
                        .category("BALLOON_DECOR")
                        .packageName("Pastel Balloon Garland & Photo Backdrop")
                        .description("Luxury organic balloon arch, personalized acrylic ring backdrop, fairy lights & photo props.")
                        .priceRange("₹2,500 - ₹4,500 fixed")
                        .contactPerson("DreamParty Decorators")
                        .contactPhone("+91 98765 33445")
                        .sampleMenu("6ft Organic Balloon Arch, LED Neon Sign, Custom Selfie Photo Props")
                        .rating(BigDecimal.valueOf(4.80))
                        .isActive(true)
                        .build(),

                KittyPartyVendorPackage.builder()
                        .community(community)
                        .category("EMCEE_ENTERTAINMENT")
                        .packageName("Live Housie Host & Party Anchor")
                        .description("Professional interactive host for Tambola, music games, trivia, and ice-breakers with wireless mic & sound.")
                        .priceRange("₹3,000 / event")
                        .contactPerson("Anchor Priya V")
                        .contactPhone("+91 98765 55667")
                        .sampleMenu("2-hour entertaining engagement, 90-ball Tambola caller, dance party playlist")
                        .rating(BigDecimal.valueOf(4.95))
                        .isActive(true)
                        .build(),

                KittyPartyVendorPackage.builder()
                        .community(community)
                        .category("PHOTOGRAPHY")
                        .packageName("Party Memories Photo & Reel Package")
                        .description("Candid photographer + 1 edited Instagram reel, all high-res digital photos delivered within 24h.")
                        .priceRange("₹3,500 fixed")
                        .contactPerson("Aperture Studios")
                        .contactPhone("+91 98765 77889")
                        .sampleMenu("2-hour candid coverage, 50 edited portraits, 1 trending Instagram Reel")
                        .rating(BigDecimal.valueOf(4.75))
                        .isActive(true)
                        .build()
        );

        packageRepository.saveAll(defaults);
        log.info("Seeded 5 default party vendor packages for community id={}", community.getId());
    }

    private VendorPackageResponse toResponse(KittyPartyVendorPackage p) {
        return VendorPackageResponse.builder()
                .id(p.getId())
                .communityId(p.getCommunity().getId())
                .vendorId(p.getVendor() != null ? p.getVendor().getId() : null)
                .category(p.getCategory())
                .packageName(p.getPackageName())
                .description(p.getDescription())
                .priceRange(p.getPriceRange())
                .contactPerson(p.getContactPerson())
                .contactPhone(p.getContactPhone())
                .sampleMenu(p.getSampleMenu())
                .rating(p.getRating())
                .isActive(p.getIsActive())
                .build();
    }
}

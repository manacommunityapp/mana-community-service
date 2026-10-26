package com.manacommunity.api.commerce.customer.service;

import com.manacommunity.api.commerce.customer.dto.*;
import com.manacommunity.api.commerce.customer.model.CommerceCustomerAddress;
import com.manacommunity.api.commerce.customer.model.CommerceCustomerProfile;
import com.manacommunity.api.commerce.customer.repository.CommerceCustomerAddressRepository;
import com.manacommunity.api.commerce.customer.repository.CommerceCustomerProfileRepository;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.repository.CommunityRepository;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommerceCustomerProfileService {

    private final CommerceCustomerProfileRepository profileRepo;
    private final CommerceCustomerAddressRepository addressRepo;
    private final CommunityRepository communityRepo;

    // ── Get or auto-create the caller's profile ──────────────────────────────

    @Transactional
    public CommerceCustomerProfileResponse getOrCreateMyProfile(AppUser user) {
        CommerceCustomerProfile profile = profileRepo
                .findByUserIdAndCommunityId(user.getId(), user.getCommunity().getId())
                .orElseGet(() -> createDefault(user));
        return toResponse(profile);
    }

    // ── Update display name / phone / preferences ─────────────────────────────

    @Transactional
    public CommerceCustomerProfileResponse updateMyProfile(AppUser user,
                                                            CommerceCustomerProfileUpdateRequest req) {
        CommerceCustomerProfile profile = getOrCreate(user);
        if (req.getDisplayName()        != null) profile.setDisplayName(req.getDisplayName());
        if (req.getPhoneNumber()        != null) profile.setPhoneNumber(req.getPhoneNumber());
        if (req.getPreferredCategories() != null) profile.setPreferredCategories(req.getPreferredCategories());
        return toResponse(profileRepo.save(profile));
    }

    // ── Address CRUD ──────────────────────────────────────────────────────────

    @Transactional
    public CommerceCustomerAddressResponse addAddress(AppUser user, CommerceCustomerAddressRequest req) {
        CommerceCustomerProfile profile = getOrCreate(user);

        CommerceCustomerAddress.AddressLabel label = parseLabel(req.getLabel());
        boolean makeDefault = Boolean.TRUE.equals(req.getIsDefault()) || profile.getAddresses().isEmpty();

        if (makeDefault) {
            addressRepo.clearDefaultsForCustomer(profile.getId());
        }

        CommerceCustomerAddress address = CommerceCustomerAddress.builder()
                .customer(profile)
                .label(label)
                .addressLine1(req.getAddressLine1())
                .addressLine2(req.getAddressLine2())
                .landmark(req.getLandmark())
                .city(req.getCity())
                .state(req.getState())
                .pincode(req.getPincode())
                .latitude(req.getLatitude() != null ? BigDecimal.valueOf(req.getLatitude()) : null)
                .longitude(req.getLongitude() != null ? BigDecimal.valueOf(req.getLongitude()) : null)
                .isDefault(makeDefault)
                .build();

        address = addressRepo.save(address);

        if (makeDefault) {
            profile.setDefaultAddressId(address.getId());
            profileRepo.save(profile);
        }

        return toAddressResponse(address);
    }

    @Transactional
    public CommerceCustomerAddressResponse updateAddress(AppUser user, Long addressId,
                                                          CommerceCustomerAddressRequest req) {
        CommerceCustomerAddress address = addressRepo.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", addressId));

        validateOwnership(user, address);

        if (req.getLabel()        != null) address.setLabel(parseLabel(req.getLabel()));
        if (req.getAddressLine1() != null) address.setAddressLine1(req.getAddressLine1());
        if (req.getAddressLine2() != null) address.setAddressLine2(req.getAddressLine2());
        if (req.getLandmark()     != null) address.setLandmark(req.getLandmark());
        if (req.getCity()         != null) address.setCity(req.getCity());
        if (req.getState()        != null) address.setState(req.getState());
        if (req.getPincode()      != null) address.setPincode(req.getPincode());
        if (req.getLatitude()     != null) address.setLatitude(BigDecimal.valueOf(req.getLatitude()));
        if (req.getLongitude()    != null) address.setLongitude(BigDecimal.valueOf(req.getLongitude()));

        if (Boolean.TRUE.equals(req.getIsDefault())) {
            addressRepo.clearDefaultsForCustomer(address.getCustomer().getId());
            address.setIsDefault(true);
            address.getCustomer().setDefaultAddressId(address.getId());
            profileRepo.save(address.getCustomer());
        }

        return toAddressResponse(addressRepo.save(address));
    }

    @Transactional
    public void deleteAddress(AppUser user, Long addressId) {
        CommerceCustomerAddress address = addressRepo.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", addressId));
        validateOwnership(user, address);

        CommerceCustomerProfile profile = address.getCustomer();
        if (Boolean.TRUE.equals(address.getIsDefault())) {
            profile.setDefaultAddressId(null);
            profileRepo.save(profile);
        }
        addressRepo.delete(address);
    }

    // ── Admin: update loyalty points & stats ─────────────────────────────────

    @Transactional
    public void recordOrderCompletion(Long userId, Long communityId, BigDecimal orderAmount) {
        profileRepo.findByUserIdAndCommunityId(userId, communityId).ifPresent(p -> {
            p.setTotalOrders(p.getTotalOrders() + 1);
            p.setTotalSpend(p.getTotalSpend().add(orderAmount));
            BigDecimal earned = orderAmount.multiply(BigDecimal.valueOf(0.01)); // 1% loyalty
            p.setLoyaltyPoints(p.getLoyaltyPoints().add(earned));
            profileRepo.save(p);
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private CommerceCustomerProfile getOrCreate(AppUser user) {
        return profileRepo.findByUserIdAndCommunityId(user.getId(), user.getCommunity().getId())
                .orElseGet(() -> createDefault(user));
    }

    private CommerceCustomerProfile createDefault(AppUser user) {
        Community community = communityRepo.findById(user.getCommunity().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Community", user.getCommunity().getId()));

        CommerceCustomerProfile p = CommerceCustomerProfile.builder()
                .user(user)
                .community(community)
                .displayName(user.getFullName())
                .customerStatus(CommerceCustomerProfile.CustomerStatus.ACTIVE)
                .isMember(true)
                .build();
        return profileRepo.save(p);
    }

    private void validateOwnership(AppUser user, CommerceCustomerAddress address) {
        if (!address.getCustomer().getUser().getId().equals(user.getId())) {
            throw new SecurityException("Address does not belong to the requesting user.");
        }
    }

    private CommerceCustomerAddress.AddressLabel parseLabel(String raw) {
        if (raw == null) return CommerceCustomerAddress.AddressLabel.HOME;
        try {
            return CommerceCustomerAddress.AddressLabel.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            return CommerceCustomerAddress.AddressLabel.OTHER;
        }
    }

    private CommerceCustomerProfileResponse toResponse(CommerceCustomerProfile p) {
        List<CommerceCustomerAddressResponse> addrs = p.getAddresses() == null ? List.of()
                : p.getAddresses().stream().map(this::toAddressResponse).collect(Collectors.toList());

        return CommerceCustomerProfileResponse.builder()
                .id(p.getId())
                .userId(p.getUser().getId())
                .displayName(p.getDisplayName())
                .email(p.getUser().getEmail())
                .phoneNumber(p.getPhoneNumber())
                .customerStatus(p.getCustomerStatus() != null ? p.getCustomerStatus().name() : null)
                .loyaltyPoints(p.getLoyaltyPoints())
                .totalOrders(p.getTotalOrders())
                .totalSpend(p.getTotalSpend())
                .isMember(p.getIsMember())
                .preferredCategories(p.getPreferredCategories())
                .kycVerified(p.getKycVerified())
                .kycVerifiedAt(p.getKycVerifiedAt())
                .defaultAddressId(p.getDefaultAddressId())
                .addresses(addrs)
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }

    private CommerceCustomerAddressResponse toAddressResponse(CommerceCustomerAddress a) {
        return CommerceCustomerAddressResponse.builder()
                .id(a.getId())
                .label(a.getLabel() != null ? a.getLabel().name() : null)
                .addressLine1(a.getAddressLine1())
                .addressLine2(a.getAddressLine2())
                .landmark(a.getLandmark())
                .city(a.getCity())
                .state(a.getState())
                .pincode(a.getPincode())
                .latitude(a.getLatitude())
                .longitude(a.getLongitude())
                .isDefault(a.getIsDefault())
                .createdAt(a.getCreatedAt())
                .build();
    }
}

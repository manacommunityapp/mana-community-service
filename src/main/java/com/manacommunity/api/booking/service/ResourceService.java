package com.manacommunity.api.booking.service;

import com.manacommunity.api.booking.dto.ResourceRequest;
import com.manacommunity.api.booking.dto.ResourceResponse;
import com.manacommunity.api.booking.entity.Resource;
import com.manacommunity.api.booking.entity.ResourceCategory;
import com.manacommunity.api.booking.entity.ResourceImage;
import com.manacommunity.api.booking.entity.enums.BookingType;
import com.manacommunity.api.booking.entity.enums.ResourceStatus;
import com.manacommunity.api.booking.repository.ResourceCategoryRepository;
import com.manacommunity.api.booking.repository.ResourceImageRepository;
import com.manacommunity.api.booking.repository.ResourceRepository;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ResourceService {

    private final ResourceRepository resourceRepo;
    private final ResourceCategoryRepository categoryRepo;
    private final ResourceImageRepository imageRepo;

    @Transactional(readOnly = true)
    public List<ResourceResponse> getResources(Long communityId, Long categoryId) {
        List<Resource> resources;
        if (categoryId != null) {
            resources = resourceRepo.findByCommunityIdAndCategoryIdAndStatusAndDeletedFalse(
                    communityId, categoryId, ResourceStatus.ACTIVE);
        } else {
            resources = resourceRepo.findByCommunityIdAndStatusAndDeletedFalseOrderByNameAsc(
                    communityId, ResourceStatus.ACTIVE);
        }
        return resources.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ResourceResponse getResource(Long id) {
        Resource resource = resourceRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Resource not found: " + id));
        return toResponse(resource);
    }

    @Transactional
    public ResourceResponse createResource(ResourceRequest req, Community community, AppUser user) {
        ResourceCategory category = categoryRepo.findById(req.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Resource category not found: " + req.getCategoryId()));

        Resource resource = Resource.builder()
                .name(req.getName())
                .category(category)
                .description(req.getDescription())
                .capacity(req.getCapacity())
                .location(req.getLocation())
                .building(req.getBuilding())
                .floor(req.getFloor())
                .bookingDurationMinutes(req.getBookingDurationMinutes())
                .minimumDurationMinutes(req.getMinimumDurationMinutes())
                .maximumDurationMinutes(req.getMaximumDurationMinutes())
                .bufferTimeMinutes(req.getBufferTimeMinutes())
                .cleaningTimeMinutes(req.getCleaningTimeMinutes())
                .advanceBookingDays(req.getAdvanceBookingDays())
                .maxBookingsPerUser(req.getMaxBookingsPerUser())
                .maxActiveBookings(req.getMaxActiveBookings())
                .cancellationHours(req.getCancellationHours())
                .maxCapacity(req.getMaxCapacity())
                .community(community)
                .createdBy(user)
                .build();

        if (req.getLatitude() != null) resource.setLatitude(req.getLatitude().doubleValue());
        if (req.getLongitude() != null) resource.setLongitude(req.getLongitude().doubleValue());
        if (req.getOpenTime() != null && !req.getOpenTime().isBlank()) {
            resource.setOpenTime(LocalTime.parse(req.getOpenTime()));
        }
        if (req.getCloseTime() != null && !req.getCloseTime().isBlank()) {
            resource.setCloseTime(LocalTime.parse(req.getCloseTime()));
        }
        if (req.getAutoCancel() != null) resource.setAutoCancel(req.getAutoCancel());
        if (req.getApprovalRequired() != null) resource.setApprovalRequired(req.getApprovalRequired());
        if (req.getDepositRequired() != null) resource.setDepositRequired(req.getDepositRequired());
        if (req.getPaymentRequired() != null) resource.setPaymentRequired(req.getPaymentRequired());
        if (req.getAllowWaitlist() != null) resource.setAllowWaitlist(req.getAllowWaitlist());
        if (req.getAllowGuest() != null) resource.setAllowGuest(req.getAllowGuest());
        if (req.getQrCheckIn() != null) resource.setQrCheckIn(req.getQrCheckIn());
        if (req.getRecurringBookingAllowed() != null) resource.setRecurringBookingAllowed(req.getRecurringBookingAllowed());
        if (req.getBookingType() != null && !req.getBookingType().isBlank()) {
            resource.setBookingType(parseEnum(BookingType.class, req.getBookingType()));
        }
        if (req.getStatus() != null && !req.getStatus().isBlank()) {
            resource.setStatus(parseEnum(ResourceStatus.class, req.getStatus()));
        }

        return toResponse(resourceRepo.save(resource));
    }

    @Transactional
    public ResourceResponse updateResource(Long id, ResourceRequest req) {
        Resource resource = resourceRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Resource not found: " + id));

        if (req.getName() != null) resource.setName(req.getName());
        if (req.getCategoryId() != null) {
            ResourceCategory category = categoryRepo.findById(req.getCategoryId())
                    .orElseThrow(() -> new IllegalArgumentException("Resource category not found: " + req.getCategoryId()));
            resource.setCategory(category);
        }
        if (req.getDescription() != null) resource.setDescription(req.getDescription());
        if (req.getCapacity() != null) resource.setCapacity(req.getCapacity());
        if (req.getLocation() != null) resource.setLocation(req.getLocation());
        if (req.getBuilding() != null) resource.setBuilding(req.getBuilding());
        if (req.getFloor() != null) resource.setFloor(req.getFloor());
        if (req.getLatitude() != null) resource.setLatitude(req.getLatitude().doubleValue());
        if (req.getLongitude() != null) resource.setLongitude(req.getLongitude().doubleValue());
        if (req.getOpenTime() != null && !req.getOpenTime().isBlank()) {
            resource.setOpenTime(LocalTime.parse(req.getOpenTime()));
        }
        if (req.getCloseTime() != null && !req.getCloseTime().isBlank()) {
            resource.setCloseTime(LocalTime.parse(req.getCloseTime()));
        }
        if (req.getBookingDurationMinutes() != null) resource.setBookingDurationMinutes(req.getBookingDurationMinutes());
        if (req.getMinimumDurationMinutes() != null) resource.setMinimumDurationMinutes(req.getMinimumDurationMinutes());
        if (req.getMaximumDurationMinutes() != null) resource.setMaximumDurationMinutes(req.getMaximumDurationMinutes());
        if (req.getBufferTimeMinutes() != null) resource.setBufferTimeMinutes(req.getBufferTimeMinutes());
        if (req.getCleaningTimeMinutes() != null) resource.setCleaningTimeMinutes(req.getCleaningTimeMinutes());
        if (req.getAdvanceBookingDays() != null) resource.setAdvanceBookingDays(req.getAdvanceBookingDays());
        if (req.getMaxBookingsPerUser() != null) resource.setMaxBookingsPerUser(req.getMaxBookingsPerUser());
        if (req.getMaxActiveBookings() != null) resource.setMaxActiveBookings(req.getMaxActiveBookings());
        if (req.getCancellationHours() != null) resource.setCancellationHours(req.getCancellationHours());
        if (req.getMaxCapacity() != null) resource.setMaxCapacity(req.getMaxCapacity());
        if (req.getAutoCancel() != null) resource.setAutoCancel(req.getAutoCancel());
        if (req.getApprovalRequired() != null) resource.setApprovalRequired(req.getApprovalRequired());
        if (req.getDepositRequired() != null) resource.setDepositRequired(req.getDepositRequired());
        if (req.getPaymentRequired() != null) resource.setPaymentRequired(req.getPaymentRequired());
        if (req.getAllowWaitlist() != null) resource.setAllowWaitlist(req.getAllowWaitlist());
        if (req.getAllowGuest() != null) resource.setAllowGuest(req.getAllowGuest());
        if (req.getQrCheckIn() != null) resource.setQrCheckIn(req.getQrCheckIn());
        if (req.getRecurringBookingAllowed() != null) resource.setRecurringBookingAllowed(req.getRecurringBookingAllowed());
        if (req.getBookingType() != null && !req.getBookingType().isBlank()) {
            resource.setBookingType(parseEnum(BookingType.class, req.getBookingType()));
        }
        if (req.getStatus() != null && !req.getStatus().isBlank()) {
            resource.setStatus(parseEnum(ResourceStatus.class, req.getStatus()));
        }

        return toResponse(resourceRepo.save(resource));
    }

    @Transactional
    public void deleteResource(Long id) {
        Resource resource = resourceRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Resource not found: " + id));
        resource.setDeleted(true);
        resourceRepo.save(resource);
    }

    private ResourceResponse toResponse(Resource r) {
        String primaryImageUrl = null;
        ResourceImage primaryImage = imageRepo.findByResourceIdAndIsPrimaryTrue(r.getId()).orElse(null);
        if (primaryImage != null) {
            primaryImageUrl = primaryImage.getImageUrl();
        }

        ResourceCategory cat = r.getCategory();
        return ResourceResponse.builder()
                .id(r.getId())
                .name(r.getName())
                .categoryId(cat != null ? cat.getId() : null)
                .categoryName(cat != null ? cat.getName() : null)
                .categoryIcon(cat != null ? cat.getIcon() : null)
                .categoryColor(cat != null ? cat.getColor() : null)
                .description(r.getDescription())
                .capacity(r.getCapacity())
                .location(r.getLocation())
                .building(r.getBuilding())
                .floor(r.getFloor())
                .latitude(r.getLatitude() != null ? java.math.BigDecimal.valueOf(r.getLatitude()) : null)
                .longitude(r.getLongitude() != null ? java.math.BigDecimal.valueOf(r.getLongitude()) : null)
                .openTime(r.getOpenTime() != null ? r.getOpenTime().format(DateTimeFormatter.ISO_LOCAL_TIME) : null)
                .closeTime(r.getCloseTime() != null ? r.getCloseTime().format(DateTimeFormatter.ISO_LOCAL_TIME) : null)
                .bookingDurationMinutes(r.getBookingDurationMinutes())
                .minimumDurationMinutes(r.getMinimumDurationMinutes())
                .maximumDurationMinutes(r.getMaximumDurationMinutes())
                .bufferTimeMinutes(r.getBufferTimeMinutes())
                .cleaningTimeMinutes(r.getCleaningTimeMinutes())
                .advanceBookingDays(r.getAdvanceBookingDays())
                .maxBookingsPerUser(r.getMaxBookingsPerUser())
                .maxActiveBookings(r.getMaxActiveBookings())
                .cancellationHours(r.getCancellationHours())
                .autoCancel(r.isAutoCancel())
                .approvalRequired(r.isApprovalRequired())
                .depositRequired(r.isDepositRequired())
                .paymentRequired(r.isPaymentRequired())
                .allowWaitlist(r.isAllowWaitlist())
                .allowGuest(r.isAllowGuest())
                .qrCheckIn(r.isQrCheckIn())
                .recurringBookingAllowed(r.isRecurringBookingAllowed())
                .maxCapacity(r.getMaxCapacity())
                .bookingType(r.getBookingType() != null ? r.getBookingType().name() : null)
                .status(r.getStatus() != null ? r.getStatus().name() : null)
                .deleted(r.isDeleted())
                .primaryImageUrl(primaryImageUrl)
                .communityId(r.getCommunity() != null ? r.getCommunity().getId() : null)
                .createdAt(r.getCreatedAt() != null ? r.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null)
                .updatedAt(r.getUpdatedAt() != null ? r.getUpdatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null)
                .build();
    }

    private <E extends Enum<E>> E parseEnum(Class<E> enumClass, String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Enum.valueOf(enumClass, value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid value '" + value + "' for " + enumClass.getSimpleName());
        }
    }
}

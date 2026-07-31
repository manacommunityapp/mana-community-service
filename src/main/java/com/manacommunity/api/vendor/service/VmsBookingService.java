package com.manacommunity.api.vendor.service;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.vendor.dto.BookingRequest;
import com.manacommunity.api.vendor.dto.BookingResponse;
import com.manacommunity.api.vendor.entity.*;
import com.manacommunity.api.vendor.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VmsBookingService {

    private final VmsBookingRepository bookingRepo;
    private final VmsVendorRepository vendorRepo;
    private final VmsVendorServiceRepository serviceRepo;

    @Transactional(readOnly = true)
    public Page<BookingResponse> getCommunityBookings(Long communityId, String status, Pageable pageable) {
        if (status != null && !status.isBlank()) {
            return bookingRepo.findByCommunityIdAndStatus(communityId, VmsBooking.BookingStatus.valueOf(status), pageable)
                    .map(this::toResponse);
        }
        return bookingRepo.findByCommunityId(communityId, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<BookingResponse> getVendorBookings(Long vendorId, String status, Pageable pageable) {
        if (status != null && !status.isBlank()) {
            return bookingRepo.findByVendorIdAndStatus(vendorId, VmsBooking.BookingStatus.valueOf(status), pageable)
                    .map(this::toResponse);
        }
        return bookingRepo.findByVendorId(vendorId, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<BookingResponse> getUserBookings(Long userId, String status, Pageable pageable) {
        if (status != null && !status.isBlank()) {
            return bookingRepo.findByResidentIdAndStatus(userId, VmsBooking.BookingStatus.valueOf(status), pageable)
                    .map(this::toResponse);
        }
        return bookingRepo.findByResidentId(userId, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public BookingResponse getById(Long id, Long communityId) {
        return toResponse(bookingRepo.findByIdAndCommunityId(id, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", id)));
    }

    @Transactional
    public BookingResponse create(BookingRequest req, AppUser user, Community community) {
        VmsVendor vendor = vendorRepo.findById(req.getVendorId())
                .orElseThrow(() -> new ResourceNotFoundException("Vendor", req.getVendorId()));
        VmsVendorService service = serviceRepo.findById(req.getServiceId())
                .orElseThrow(() -> new ResourceNotFoundException("Service", req.getServiceId()));

        String bookingNumber = "BK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        VmsBooking booking = VmsBooking.builder()
                .bookingNumber(bookingNumber)
                .vendor(vendor)
                .resident(user)
                .bookingType(req.getBookingType() != null ? VmsBooking.BookingType.valueOf(req.getBookingType()) : VmsBooking.BookingType.STANDARD)
                .scheduledDate(req.getScheduledDate())
                .scheduledTime(req.getScheduledTime())
                .address(req.getAddress())
                .specialInstructions(req.getNotes())
                .totalAmount(service.getBasePrice())
                .paymentMethod(req.getPaymentMethod())
                .community(community)
                .build();

        return toResponse(bookingRepo.save(booking));
    }

    @Transactional
    public BookingResponse updateStatus(Long id, String status, Long communityId, String reason) {
        VmsBooking booking = bookingRepo.findByIdAndCommunityId(id, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", id));
        booking.setStatus(VmsBooking.BookingStatus.valueOf(status));
        if ("CANCELLED".equals(status) && reason != null) {
            booking.setCancellationReason(reason);
        }
        return toResponse(bookingRepo.save(booking));
    }

    private BookingResponse toResponse(VmsBooking b) {
        VmsVendor v = b.getVendor();
        AppUser u = b.getResident();
        return BookingResponse.builder()
                .id(b.getId())
                .bookingNumber(b.getBookingNumber())
                .vendor(BookingResponse.VendorRef.builder()
                        .id(v.getId()).businessName(v.getBusinessName())
                        .logoUrl(v.getLogoUrl()).phone(v.getUser() != null ? v.getUser().getPhone() : null).build())
                .user(BookingResponse.UserRef.builder()
                        .id(u.getId()).fullName(u.getFullName()).phone(u.getPhone()).build())
                .bookingType(b.getBookingType() != null ? b.getBookingType().name() : null)
                .status(b.getStatus().name())
                .scheduledDate(b.getScheduledDate())
                .scheduledTime(b.getScheduledTime())
                .totalAmount(b.getTotalAmount())
                .taxAmount(b.getTaxAmount())
                .discountAmount(b.getDiscountAmount())
                .netAmount(b.getTotalAmount())
                .paymentStatus(b.getPaymentStatus() != null ? b.getPaymentStatus().name() : null)
                .paymentMethod(b.getPaymentMethod())
                .address(b.getAddress())
                .notes(b.getSpecialInstructions())
                .cancellationReason(b.getCancellationReason())
                .communityId(b.getCommunity() != null ? b.getCommunity().getId() : null)
                .createdAt(b.getCreatedAt())
                .updatedAt(b.getUpdatedAt())
                .build();
    }
}

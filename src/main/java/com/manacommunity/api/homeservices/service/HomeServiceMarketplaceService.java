package com.manacommunity.api.homeservices.service;

import com.manacommunity.api.homeservices.entity.HomeServiceBooking;
import com.manacommunity.api.homeservices.entity.HomeServiceCategory;
import com.manacommunity.api.homeservices.entity.HomeServiceProvider;
import com.manacommunity.api.homeservices.repository.HomeServiceBookingRepository;
import com.manacommunity.api.homeservices.repository.HomeServiceCategoryRepository;
import com.manacommunity.api.homeservices.repository.HomeServiceProviderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HomeServiceMarketplaceService {

    private final HomeServiceCategoryRepository categoryRepository;
    private final HomeServiceProviderRepository providerRepository;
    private final HomeServiceBookingRepository bookingRepository;

    public List<HomeServiceCategory> getCategories() {
        return categoryRepository.findByIsActiveTrue();
    }

    public List<HomeServiceProvider> getProviders(Long communityId, Long categoryId) {
        if (categoryId != null) {
            return providerRepository.findByCommunityIdAndCategoryIdAndIsAvailableTrue(communityId, categoryId);
        }
        return providerRepository.findByCommunityId(communityId);
    }

    public List<HomeServiceProvider> getProvidersByCategory(Long communityId, Long categoryId) {
        return providerRepository.findByCommunityIdAndCategoryIdAndIsAvailableTrue(communityId, categoryId);
    }

    public HomeServiceProvider getProviderById(Long id) {
        return providerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Provider not found: " + id));
    }

    @Transactional
    public HomeServiceBooking bookService(Long communityId, Long residentUserId, Long providerId,
                                          LocalDate date, LocalTime time, String description, String flatNo) {
        HomeServiceProvider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new RuntimeException("Provider not found: " + providerId));

        HomeServiceBooking booking = HomeServiceBooking.builder()
                .communityId(communityId)
                .residentUserId(residentUserId)
                .provider(provider)
                .flatNo(flatNo)
                .scheduledDate(date)
                .scheduledTime(time)
                .description(description)
                .status("REQUESTED")
                .paymentStatus("PENDING")
                .build();

        return bookingRepository.save(booking);
    }

    @Transactional
    public HomeServiceBooking updateBookingStatus(Long bookingId, String status) {
        HomeServiceBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found: " + bookingId));
        booking.setStatus(status);
        booking.setUpdatedAt(LocalDateTime.now());
        if ("COMPLETED".equals(status)) {
            booking.setCompletedAt(LocalDateTime.now());
        }
        return bookingRepository.save(booking);
    }

    @Transactional
    public HomeServiceBooking completeBookingWithRating(Long bookingId, Long residentUserId,
                                                        Integer rating, String review, BigDecimal amount) {
        HomeServiceBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found: " + bookingId));

        booking.setStatus("COMPLETED");
        booking.setResidentRating(rating);
        booking.setResidentReview(review);
        booking.setAmount(amount);
        booking.setCompletedAt(LocalDateTime.now());
        booking.setUpdatedAt(LocalDateTime.now());

        // Update provider rolling average
        HomeServiceProvider provider = booking.getProvider();
        int oldCount = provider.getRatingCount();
        BigDecimal oldRating = provider.getRating();
        BigDecimal newRating = oldRating.multiply(BigDecimal.valueOf(oldCount))
                .add(BigDecimal.valueOf(rating))
                .divide(BigDecimal.valueOf(oldCount + 1), 2, RoundingMode.HALF_UP);
        provider.setRating(newRating);
        provider.setRatingCount(oldCount + 1);
        provider.setUpdatedAt(LocalDateTime.now());
        providerRepository.save(provider);

        return bookingRepository.save(booking);
    }

    @Transactional
    public HomeServiceBooking cancelBooking(Long bookingId, String reason) {
        HomeServiceBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found: " + bookingId));
        booking.setStatus("CANCELLED");
        booking.setCancelledReason(reason);
        booking.setUpdatedAt(LocalDateTime.now());
        return bookingRepository.save(booking);
    }

    public List<HomeServiceBooking> getResidentBookings(Long communityId, Long residentUserId) {
        return bookingRepository.findByCommunityIdAndResidentUserId(communityId, residentUserId);
    }

    public List<HomeServiceBooking> getAllBookings(Long communityId, String status) {
        if (status != null && !status.isBlank()) {
            return bookingRepository.findByCommunityIdAndStatus(communityId, status);
        }
        return bookingRepository.findByCommunityId(communityId);
    }
}

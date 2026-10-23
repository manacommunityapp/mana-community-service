package com.manacommunity.api.marketplace.service;

import com.manacommunity.api.exception.ManaCommunityException;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.marketplace.entity.MarketInventoryReservation;
import com.manacommunity.api.marketplace.entity.MarketListing;
import com.manacommunity.api.marketplace.repository.MarketInventoryReservationRepository;
import com.manacommunity.api.marketplace.repository.MarketListingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketInventoryEngine {

    private final MarketInventoryReservationRepository reservationRepository;
    private final MarketListingRepository listingRepository;

    private static final int DEFAULT_HOLD_MINUTES = 10;

    @Transactional
    public MarketInventoryReservation reserveStock(Long listingId, Long userId, int quantity, String orderNumber) {
        MarketListing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found: " + listingId));

        if (listing.getStatus() != MarketListing.ListingStatus.ACTIVE) {
            throw new ManaCommunityException("Listing is no longer active for purchase.", HttpStatus.CONFLICT, "LISTING_INACTIVE");
        }

        int activeReserved = reservationRepository.countActiveReservedQuantity(listingId, LocalDateTime.now());
        int available = (listing.getAvailableQuantity() != null ? listing.getAvailableQuantity() : 1) - activeReserved;

        if (available < quantity) {
            throw new ManaCommunityException(
                    "Insufficient stock available. Requested: " + quantity + ", Available: " + Math.max(available, 0),
                    HttpStatus.CONFLICT,
                    "INSUFFICIENT_STOCK"
            );
        }

        MarketInventoryReservation reservation = MarketInventoryReservation.builder()
                .listingId(listingId)
                .userId(userId)
                .orderNumber(orderNumber)
                .quantity(quantity)
                .status(MarketInventoryReservation.ReservationStatus.RESERVED)
                .reservedUntil(LocalDateTime.now().plusMinutes(DEFAULT_HOLD_MINUTES))
                .build();

        log.info("Reserved stock: listingId={}, userId={}, qty={}, holdMinutes={}",
                listingId, userId, quantity, DEFAULT_HOLD_MINUTES);
        return reservationRepository.save(reservation);
    }

    @Transactional
    public void commitReservation(String orderNumber) {
        List<MarketInventoryReservation> reservations = reservationRepository.findByOrderNumber(orderNumber);
        for (var res : reservations) {
            if (res.getStatus() == MarketInventoryReservation.ReservationStatus.RESERVED) {
                res.setStatus(MarketInventoryReservation.ReservationStatus.COMMITTED);
                reservationRepository.save(res);

                listingRepository.findById(res.getListingId()).ifPresent(listing -> {
                    if (listing.getAvailableQuantity() != null) {
                        int remaining = Math.max(0, listing.getAvailableQuantity() - res.getQuantity());
                        listing.setAvailableQuantity(remaining);
                        if (remaining == 0) {
                            listing.setStatus(MarketListing.ListingStatus.SOLD);
                        }
                        listingRepository.save(listing);
                    }
                });
            }
        }
        log.info("Committed stock reservations for order {}", orderNumber);
    }

    @Transactional
    public void releaseReservation(String orderNumber) {
        List<MarketInventoryReservation> reservations = reservationRepository.findByOrderNumber(orderNumber);
        for (var res : reservations) {
            if (res.getStatus() == MarketInventoryReservation.ReservationStatus.RESERVED ||
                res.getStatus() == MarketInventoryReservation.ReservationStatus.COMMITTED) {
                res.setStatus(MarketInventoryReservation.ReservationStatus.RELEASED);
                reservationRepository.save(res);

                listingRepository.findById(res.getListingId()).ifPresent(listing -> {
                    if (listing.getAvailableQuantity() != null) {
                        listing.setAvailableQuantity(listing.getAvailableQuantity() + res.getQuantity());
                        if (listing.getStatus() == MarketListing.ListingStatus.SOLD) {
                            listing.setStatus(MarketListing.ListingStatus.ACTIVE);
                        }
                        listingRepository.save(listing);
                    }
                });
            }
        }
        log.info("Released stock reservations for order {}", orderNumber);
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void cleanExpiredReservations() {
        int expired = reservationRepository.expireOutdatedReservations(LocalDateTime.now());
        if (expired > 0) {
            log.info("Expired {} outdated marketplace inventory reservations", expired);
        }
    }
}

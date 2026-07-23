package com.manacommunity.api.booking.repository;

import com.manacommunity.api.booking.entity.BookingAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface BookingAnalyticsRepository extends JpaRepository<BookingAnalytics, Long> {

    List<BookingAnalytics> findByResourceIdAndAnalyticsDateBetween(Long resourceId, LocalDate from, LocalDate to);

    List<BookingAnalytics> findByCommunityIdAndAnalyticsDateBetweenOrderByAnalyticsDateAsc(
            Long communityId, LocalDate from, LocalDate to);
}

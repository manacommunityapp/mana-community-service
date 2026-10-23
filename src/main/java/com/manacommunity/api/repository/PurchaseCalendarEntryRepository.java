package com.manacommunity.api.repository;

import com.manacommunity.api.model.PurchaseCalendarEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseCalendarEntryRepository extends JpaRepository<PurchaseCalendarEntry, Long> {
    List<PurchaseCalendarEntry> findByCommunityIdAndIsActiveTrueOrderByNextDueDateAsc(Long communityId);
    List<PurchaseCalendarEntry> findByCommunityIdAndIsActiveTrueAndNextDueDateBefore(Long communityId, LocalDate before);
    Optional<PurchaseCalendarEntry> findByIdAndCommunityId(Long id, Long communityId);
}

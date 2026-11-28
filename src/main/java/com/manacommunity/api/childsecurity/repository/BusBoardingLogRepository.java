package com.manacommunity.api.childsecurity.repository;

import com.manacommunity.api.childsecurity.entity.BusBoardingLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BusBoardingLogRepository extends JpaRepository<BusBoardingLog, Long> {
    List<BusBoardingLog> findByBusRouteIdAndBoardingDate(Long routeId, LocalDate date);
    List<BusBoardingLog> findByChildIdOrderByBoardingDateDesc(Long childId);

    @Query("SELECT b FROM BusBoardingLog b WHERE b.boardingDate = :date AND b.boardingStatus = 'BOARDED' AND b.boardingType = 'AFTERNOON_DROP'")
    List<BusBoardingLog> findPendingDropoffs(LocalDate date);
}

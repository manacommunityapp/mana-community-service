package com.manacommunity.api.sports.repository;

import com.manacommunity.api.sports.scheduler.SportsTournamentReserveDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface SportsTournamentReserveDayRepository extends JpaRepository<SportsTournamentReserveDay, Long> {

    List<SportsTournamentReserveDay> findByConfigIdOrderByReserveDate(Long configId);

    List<SportsTournamentReserveDay> findByConfigIdAndStatus(Long configId, SportsTournamentReserveDay.ReserveDayStatus status);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM SportsTournamentReserveDay r WHERE r.config.id = :configId")
    void deleteByConfigId(@Param("configId") Long configId);
}

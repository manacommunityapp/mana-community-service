package com.manacommunity.api.sports.repository;

import com.manacommunity.api.sports.scheduler.SportsTournamentReserveDay;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SportsTournamentReserveDayRepository extends JpaRepository<SportsTournamentReserveDay, Long> {

    List<SportsTournamentReserveDay> findByConfigIdOrderByReserveDate(Long configId);

    List<SportsTournamentReserveDay> findByConfigIdAndStatus(Long configId, SportsTournamentReserveDay.ReserveDayStatus status);
}

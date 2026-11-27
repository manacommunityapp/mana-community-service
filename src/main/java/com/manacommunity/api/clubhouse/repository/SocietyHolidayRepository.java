package com.manacommunity.api.clubhouse.repository;

import com.manacommunity.api.clubhouse.entity.SocietyHolidayEntity;
import com.manacommunity.api.clubhouse.entity.SocietyHolidayId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SocietyHolidayRepository extends JpaRepository<SocietyHolidayEntity, SocietyHolidayId> {
}

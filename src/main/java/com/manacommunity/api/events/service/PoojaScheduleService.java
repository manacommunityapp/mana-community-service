package com.manacommunity.api.events.service;

import com.manacommunity.api.events.dto.PoojaScheduleDto;
import com.manacommunity.api.events.dto.PoojaScheduleRequest;
import com.manacommunity.api.events.enums.PoojaScheduleStatus;

import java.time.LocalDate;
import java.util.List;

public interface PoojaScheduleService {

    PoojaScheduleDto createSchedule(PoojaScheduleRequest request);

    PoojaScheduleDto updateSchedule(Long id, PoojaScheduleRequest request);

    PoojaScheduleDto updateStatus(Long id, PoojaScheduleStatus status);

    PoojaScheduleDto getById(Long id);

    List<PoojaScheduleDto> getByPooja(Long poojaId);

    List<PoojaScheduleDto> getByPoojaAndDate(Long poojaId, LocalDate date);

    List<LocalDate> getAvailableDates(Long poojaId);

    void deleteSchedule(Long id);
}

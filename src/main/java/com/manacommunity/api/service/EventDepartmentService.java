package com.manacommunity.api.service;

import com.manacommunity.api.dto.EventDepartmentDto;
import com.manacommunity.api.model.EventDepartment;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.repository.EventDepartmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventDepartmentService {

    private final EventDepartmentRepository repository;

    @Transactional(readOnly = true)
    public List<EventDepartmentDto> list(Long communityId, Long eventId) {
        List<EventDepartment> list;
        if (eventId != null) {
            list = repository.findByEventIdAndActiveTrueOrderByNameAsc(eventId);
        } else if (communityId != null) {
            list = repository.findByCommunityIdAndActiveTrueOrderByNameAsc(communityId);
        } else {
            list = repository.findByActiveTrueOrderByNameAsc();
        }
        return list.stream().map(EventDepartmentDto::from).toList();
    }

    @Transactional
    public EventDepartmentDto save(EventDepartmentDto dto) {
        EventDepartment dept;
        if (dto.getId() != null) {
            dept = repository.findById(dto.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department", dto.getId()));
            if (dto.getName() != null) dept.setName(dto.getName());
            if (dto.getHeadName() != null) dept.setHeadName(dto.getHeadName());
            if (dto.getTotalTarget() != null) dept.setTotalTarget(dto.getTotalTarget());
            if (dto.getPresentCount() != null) dept.setPresentCount(dto.getPresentCount());
            if (dto.getColor() != null) dept.setColor(dto.getColor());
            if (dto.getDescription() != null) dept.setDescription(dto.getDescription());
        } else {
            dept = EventDepartment.builder()
                    .communityId(dto.getCommunityId())
                    .eventId(dto.getEventId())
                    .name(dto.getName())
                    .headName(dto.getHeadName())
                    .totalTarget(dto.getTotalTarget() != null ? dto.getTotalTarget() : 10)
                    .presentCount(dto.getPresentCount() != null ? dto.getPresentCount() : 0)
                    .color(dto.getColor() != null ? dto.getColor() : "#6366f1")
                    .description(dto.getDescription())
                    .active(true)
                    .build();
        }
        return EventDepartmentDto.from(repository.save(dept));
    }

    @Transactional
    public void delete(Long id) {
        repository.findById(id).ifPresent(d -> {
            d.setActive(false);
            repository.save(d);
        });
    }
}

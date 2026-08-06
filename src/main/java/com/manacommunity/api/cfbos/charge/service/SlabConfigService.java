package com.manacommunity.api.cfbos.charge.service;

import com.manacommunity.api.cfbos.charge.dto.SlabConfigDto;
import com.manacommunity.api.cfbos.charge.entity.SlabConfig;
import com.manacommunity.api.cfbos.charge.repository.SlabConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SlabConfigService {
    private final SlabConfigRepository slabConfigRepository;

    @Transactional(readOnly = true)
    public List<SlabConfigDto> getAll() {
        return slabConfigRepository.findAll().stream().map(this::toDto).toList();
    }

    private SlabConfigDto toDto(SlabConfig e) {
        return SlabConfigDto.builder()
                .id(e.getId()).name(e.getName()).description(e.getDescription())
                .unitLabel(e.getUnitLabel()).isActive(e.getIsActive())
                .tiers(e.getTiers() != null ? e.getTiers().stream().map(t ->
                        SlabConfigDto.TierDto.builder()
                                .tierFrom(t.getTierFrom()).tierTo(t.getTierTo())
                                .rate(t.getRate()).fixedCharge(t.getFixedCharge())
                                .tierOrder(t.getTierOrder()).build()).toList() : List.of())
                .build();
    }
}

package com.manacommunity.api.cfbos.charge.service;

import com.manacommunity.api.cfbos.charge.dto.FormulaDto;
import com.manacommunity.api.cfbos.charge.entity.Formula;
import com.manacommunity.api.cfbos.charge.repository.FormulaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FormulaService {
    private final FormulaRepository formulaRepository;

    @Transactional(readOnly = true)
    public List<FormulaDto> getAll() {
        return formulaRepository.findByIsActiveTrue().stream().map(this::toDto).toList();
    }

    private FormulaDto toDto(Formula e) {
        return FormulaDto.builder()
                .id(e.getId()).name(e.getName()).expression(e.getExpression())
                .description(e.getDescription()).isActive(e.getIsActive())
                .build();
    }
}

package com.manacommunity.api.cfbos.charge.service;

import com.manacommunity.api.cfbos.charge.dto.FormulaDto;
import com.manacommunity.api.cfbos.charge.entity.Formula;
import com.manacommunity.api.cfbos.charge.entity.FormulaVariable;
import com.manacommunity.api.cfbos.charge.repository.FormulaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Collections;
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
        List<FormulaDto.VariableDto> variables = e.getVariables() != null
                ? e.getVariables().stream().map(this::toVariableDto).toList()
                : Collections.emptyList();

        return FormulaDto.builder()
                .id(e.getId()).name(e.getName()).expression(e.getExpression())
                .description(e.getDescription()).isActive(e.getIsActive())
                .variables(variables)
                .build();
    }

    private FormulaDto.VariableDto toVariableDto(FormulaVariable v) {
        return FormulaDto.VariableDto.builder()
                .variableName(v.getVariableName())
                .variableSource(v.getVariableSource())
                .sourceField(v.getSourceField())
                .defaultValue(v.getDefaultValue())
                .description(v.getDescription())
                .build();
    }
}

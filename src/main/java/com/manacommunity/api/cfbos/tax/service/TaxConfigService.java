package com.manacommunity.api.cfbos.tax.service;

import com.manacommunity.api.cfbos.shared.exception.CfbosResourceNotFoundException;
import com.manacommunity.api.cfbos.tax.dto.*;
import com.manacommunity.api.cfbos.tax.entity.*;
import com.manacommunity.api.cfbos.tax.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TaxConfigService {

    private final TaxConfigRepository taxConfigRepository;
    private final TaxRateRepository taxRateRepository;
    private final HsnSacCodeRepository hsnSacCodeRepository;
    private final TdsSectionRepository tdsSectionRepository;

    @Transactional(readOnly = true)
    public TaxConfigDto getTaxConfig() {
        TaxConfig config = taxConfigRepository.findAll().stream().findFirst()
                .orElse(TaxConfig.builder().build());
        return toDto(config);
    }

    @Transactional
    public TaxConfigDto updateTaxConfig(TaxConfigDto dto) {
        TaxConfig config = taxConfigRepository.findAll().stream().findFirst()
                .orElse(TaxConfig.builder().build());
        config.setCommunityGstin(dto.getCommunityGstin());
        config.setCommunityStateCode(dto.getCommunityStateCode());
        config.setIsGstRegistered(dto.getIsGstRegistered());
        config.setDefaultGstRate(dto.getDefaultGstRate());
        config.setDefaultCgstRate(dto.getDefaultCgstRate());
        config.setDefaultSgstRate(dto.getDefaultSgstRate());
        config.setFinancialYearStartMonth(dto.getFinancialYearStartMonth());
        return toDto(taxConfigRepository.save(config));
    }

    @Transactional(readOnly = true)
    public List<TaxRateDto> getAllTaxRates() {
        return taxRateRepository.findByIsActiveTrue().stream()
                .map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<HsnSacCodeDto> getAllHsnSacCodes() {
        return hsnSacCodeRepository.findAll().stream()
                .map(this::toDto).toList();
    }

    @Transactional
    public HsnSacCodeDto createHsnSacCode(HsnSacCodeDto dto) {
        HsnSacCode entity = HsnSacCode.builder()
                .code(dto.getCode())
                .description(dto.getDescription())
                .codeType(dto.getCodeType())
                .defaultGstRate(dto.getDefaultGstRate())
                .build();
        return toDto(hsnSacCodeRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public List<TdsSectionDto> getAllTdsSections() {
        return tdsSectionRepository.findAll().stream()
                .map(this::toDto).toList();
    }

    private TaxConfigDto toDto(TaxConfig e) {
        return TaxConfigDto.builder()
                .id(e.getId())
                .communityGstin(e.getCommunityGstin())
                .communityStateCode(e.getCommunityStateCode())
                .isGstRegistered(e.getIsGstRegistered())
                .defaultGstRate(e.getDefaultGstRate())
                .defaultCgstRate(e.getDefaultCgstRate())
                .defaultSgstRate(e.getDefaultSgstRate())
                .financialYearStartMonth(e.getFinancialYearStartMonth())
                .build();
    }

    private TaxRateDto toDto(TaxRate e) {
        return TaxRateDto.builder()
                .id(e.getId()).name(e.getName()).taxType(e.getTaxType())
                .rate(e.getRate()).cgstRate(e.getCgstRate())
                .sgstRate(e.getSgstRate()).igstRate(e.getIgstRate())
                .effectiveFrom(e.getEffectiveFrom()).effectiveTo(e.getEffectiveTo())
                .isActive(e.getIsActive()).build();
    }

    private HsnSacCodeDto toDto(HsnSacCode e) {
        return HsnSacCodeDto.builder()
                .id(e.getId()).code(e.getCode()).description(e.getDescription())
                .codeType(e.getCodeType()).defaultGstRate(e.getDefaultGstRate())
                .isActive(e.getIsActive()).build();
    }

    private TdsSectionDto toDto(TdsSection e) {
        return TdsSectionDto.builder()
                .id(e.getId()).sectionCode(e.getSectionCode()).description(e.getDescription())
                .individualRate(e.getIndividualRate()).companyRate(e.getCompanyRate())
                .thresholdAmount(e.getThresholdAmount()).isActive(e.getIsActive()).build();
    }
}

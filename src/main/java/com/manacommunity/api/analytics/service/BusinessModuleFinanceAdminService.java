package com.manacommunity.api.analytics.service;

import com.manacommunity.api.analytics.dto.BusinessModuleFinanceRequest;
import com.manacommunity.api.analytics.entity.BusinessModuleFinanceDaily;
import com.manacommunity.api.analytics.repository.BusinessModuleFinanceDailyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class BusinessModuleFinanceAdminService {
    private final BusinessModuleFinanceDailyRepository financeRepository;

    @Transactional
    public BusinessModuleFinanceDaily upsert(BusinessModuleFinanceRequest request) {
        validateAmount(request.getCommissionRevenue(), "commissionRevenue");
        validateAmount(request.getDirectCost(), "directCost");
        String moduleCode = request.getModuleCode().trim().toUpperCase();
        BusinessModuleFinanceDaily record = financeRepository.findByModuleCodeAndReportDate(moduleCode, request.getReportDate())
                .orElseGet(BusinessModuleFinanceDaily::new);
        record.setModuleCode(moduleCode);
        record.setReportDate(request.getReportDate());
        record.setCommissionRevenue(request.getCommissionRevenue().setScale(2, RoundingMode.UNNECESSARY));
        record.setDirectCost(request.getDirectCost().setScale(2, RoundingMode.UNNECESSARY));
        return financeRepository.save(record);
    }

    private void validateAmount(java.math.BigDecimal amount, String field) {
        if (amount.scale() > 2 || amount.precision() > 14) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " supports up to 12 integer and 2 fractional digits");
        }
    }
}

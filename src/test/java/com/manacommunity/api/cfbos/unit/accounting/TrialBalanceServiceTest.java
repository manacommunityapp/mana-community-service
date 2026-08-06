package com.manacommunity.api.cfbos.unit.accounting;

import com.manacommunity.api.cfbos.accounting.dto.TrialBalanceDto;
import com.manacommunity.api.cfbos.accounting.entity.Account;
import com.manacommunity.api.cfbos.accounting.entity.FiscalYear;
import com.manacommunity.api.cfbos.accounting.enums.AccountType;
import com.manacommunity.api.cfbos.accounting.repository.AccountRepository;
import com.manacommunity.api.cfbos.accounting.repository.FiscalYearRepository;
import com.manacommunity.api.cfbos.accounting.service.TrialBalanceService;
import com.manacommunity.api.cfbos.shared.exception.CfbosResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TrialBalanceService")
class TrialBalanceServiceTest {

    @Mock private AccountRepository accountRepository;
    @Mock private FiscalYearRepository fiscalYearRepository;

    @InjectMocks
    private TrialBalanceService trialBalanceService;

    @Test
    @DisplayName("should produce a balanced trial balance from account balances")
    void shouldGenerateBalancedTrialBalance() {
        FiscalYear fy = FiscalYear.builder().id(1L).name("2026-27")
                .startDate(LocalDate.of(2026, 4, 1))
                .endDate(LocalDate.of(2027, 3, 31)).build();

        Account receivable = Account.builder().id(1L).code("1120")
                .name("Resident Receivables").accountType(AccountType.ASSET)
                .currentBalance(new BigDecimal("5000.00")).isActive(true).build();
        Account income = Account.builder().id(2L).code("4100")
                .name("Maintenance Income").accountType(AccountType.INCOME)
                .currentBalance(new BigDecimal("5000.00")).isActive(true).build();
        Account zeroBalance = Account.builder().id(3L).code("1000")
                .name("Cash").accountType(AccountType.ASSET)
                .currentBalance(BigDecimal.ZERO).isActive(true).build();

        when(fiscalYearRepository.findById(1L)).thenReturn(Optional.of(fy));
        when(accountRepository.findByIsActiveTrue())
                .thenReturn(List.of(receivable, income, zeroBalance));

        TrialBalanceDto result = trialBalanceService.generate(1L);

        assertThat(result.getFiscalYearName()).isEqualTo("2026-27");
        assertThat(result.getTotalDebit()).isEqualByComparingTo("5000.00");
        assertThat(result.getTotalCredit()).isEqualByComparingTo("5000.00");
        assertThat(result.getTotalDebit()).isEqualByComparingTo(result.getTotalCredit());
        // zero-balance accounts are excluded from the trial balance lines
        assertThat(result.getLines()).hasSize(2);
        assertThat(result.getLines())
                .extracting(TrialBalanceDto.TrialBalanceLine::getAccountCode)
                .containsExactlyInAnyOrder("1120", "4100");
    }

    @Test
    @DisplayName("should throw when fiscal year does not exist")
    void shouldThrowWhenFiscalYearMissing() {
        when(fiscalYearRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> trialBalanceService.generate(99L))
                .isInstanceOf(CfbosResourceNotFoundException.class);
    }
}

package com.manacommunity.api.cfbos;

import com.manacommunity.api.cfbos.statements.dto.BalanceSheetDto;
import com.manacommunity.api.cfbos.statements.dto.CashFlowStatementDto;
import com.manacommunity.api.cfbos.statements.dto.ProfitAndLossDto;
import com.manacommunity.api.cfbos.statements.service.FinancialStatementsService;
import com.manacommunity.api.cfbos.treasury.repository.FundAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class FinancialStatementsServiceTest {

    private FinancialStatementsService service;
    private FundAccountRepository fundAccountRepository;

    @BeforeEach
    void setUp() {
        fundAccountRepository = Mockito.mock(FundAccountRepository.class);
        service = new FinancialStatementsService(fundAccountRepository);
    }

    @Test
    @DisplayName("Balance Sheet: Assets equal Total Liabilities and Accumulated Surplus")
    void testBalanceSheetBalancing() {
        BalanceSheetDto bs = service.generateBalanceSheet(1L, LocalDate.now());
        assertThat(bs).isNotNull();
        assertThat(bs.isBalanced()).isTrue();
        assertThat(bs.getTotalAssets()).isEqualByComparingTo(bs.getTotalEquityAndLiabilities());
    }

    @Test
    @DisplayName("Profit and Loss: Computes operating margin and net surplus")
    void testProfitAndLoss() {
        ProfitAndLossDto pl = service.generateProfitAndLoss(1L, LocalDate.now().minusMonths(1), LocalDate.now());
        assertThat(pl).isNotNull();
        assertThat(pl.getTotalRevenue()).isGreaterThan(BigDecimal.ZERO);
        assertThat(pl.getTotalExpenses()).isGreaterThan(BigDecimal.ZERO);
        assertThat(pl.getOperatingMarginPercent()).isGreaterThan(0.0);
    }

    @Test
    @DisplayName("Cash Flow: Operating, Investing, and Financing activities reconcile to net change")
    void testCashFlowStatement() {
        CashFlowStatementDto cf = service.generateCashFlow(1L, LocalDate.now().minusMonths(1), LocalDate.now());
        assertThat(cf).isNotNull();
        BigDecimal calculatedNet = cf.getNetCashFromOperating().add(cf.getNetCashFromInvesting()).add(cf.getNetCashFromFinancing());
        assertThat(cf.getNetChangeInCash()).isEqualByComparingTo(calculatedNet);
        assertThat(cf.getClosingCashBalance()).isEqualByComparingTo(cf.getOpeningCashBalance().add(calculatedNet));
    }
}

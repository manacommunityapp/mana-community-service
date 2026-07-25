package com.manacommunity.api.cfbos.accounting.service;

import com.manacommunity.api.cfbos.accounting.dto.TrialBalanceDto;
import com.manacommunity.api.cfbos.accounting.entity.Account;
import com.manacommunity.api.cfbos.accounting.entity.FiscalYear;
import com.manacommunity.api.cfbos.accounting.enums.AccountType;
import com.manacommunity.api.cfbos.accounting.repository.AccountRepository;
import com.manacommunity.api.cfbos.accounting.repository.FiscalYearRepository;
import com.manacommunity.api.cfbos.shared.exception.CfbosResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TrialBalanceService {

    private final AccountRepository accountRepository;
    private final FiscalYearRepository fiscalYearRepository;

    @Transactional(readOnly = true)
    public TrialBalanceDto generate(Long fiscalYearId, LocalDate asOfDate) {
        FiscalYear fy = fiscalYearRepository.findById(fiscalYearId)
                .orElseThrow(() -> new CfbosResourceNotFoundException("FiscalYear", fiscalYearId));

        List<Account> accounts = accountRepository.findByIsActiveTrue();
        List<TrialBalanceDto.TrialBalanceLine> lines = new ArrayList<>();
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;

        for (Account account : accounts) {
            BigDecimal balance = account.getCurrentBalance();
            if (balance.compareTo(BigDecimal.ZERO) == 0) continue;

            BigDecimal debitBal = BigDecimal.ZERO;
            BigDecimal creditBal = BigDecimal.ZERO;

            if (account.getAccountType() == AccountType.ASSET
                    || account.getAccountType() == AccountType.EXPENSE) {
                if (balance.compareTo(BigDecimal.ZERO) > 0) debitBal = balance;
                else creditBal = balance.negate();
            } else {
                if (balance.compareTo(BigDecimal.ZERO) > 0) creditBal = balance;
                else debitBal = balance.negate();
            }

            lines.add(TrialBalanceDto.TrialBalanceLine.builder()
                    .accountCode(account.getCode())
                    .accountName(account.getName())
                    .accountType(account.getAccountType().name())
                    .debitBalance(debitBal)
                    .creditBalance(creditBal)
                    .build());

            totalDebit = totalDebit.add(debitBal);
            totalCredit = totalCredit.add(creditBal);
        }

        return TrialBalanceDto.builder()
                .fiscalYearName(fy.getName())
                .asOfDate(asOfDate)
                .totalDebit(totalDebit)
                .totalCredit(totalCredit)
                .lines(lines)
                .build();
    }
}

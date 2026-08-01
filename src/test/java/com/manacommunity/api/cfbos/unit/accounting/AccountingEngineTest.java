package com.manacommunity.api.cfbos.unit.accounting;

import com.manacommunity.api.cfbos.accounting.dto.JournalEntryRequest;
import com.manacommunity.api.cfbos.accounting.engine.AccountingEngine;
import com.manacommunity.api.cfbos.accounting.entity.*;
import com.manacommunity.api.cfbos.accounting.enums.AccountType;
import com.manacommunity.api.cfbos.accounting.enums.JournalEntryStatus;
import com.manacommunity.api.cfbos.accounting.enums.JournalEntryType;
import com.manacommunity.api.cfbos.accounting.repository.*;
import com.manacommunity.api.cfbos.shared.enums.DocumentType;
import com.manacommunity.api.cfbos.shared.enums.SourceModule;
import com.manacommunity.api.cfbos.shared.exception.UnbalancedJournalEntryException;
import com.manacommunity.api.cfbos.shared.sequence.service.DocumentSequenceService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountingEngine")
class AccountingEngineTest {

    @Mock private JournalEntryRepository journalEntryRepository;
    @Mock private JournalLineRepository journalLineRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private FiscalYearRepository fiscalYearRepository;
    @Mock private AccountingPeriodRepository accountingPeriodRepository;
    @Mock private DocumentSequenceService documentSequenceService;

    @InjectMocks
    private AccountingEngine accountingEngine;

    @Nested
    @DisplayName("createAndPostJournalEntry")
    class CreateAndPost {

        @Test
        @DisplayName("should create and post a balanced journal entry")
        void shouldCreateBalancedEntry() {
            Account receivable = Account.builder().id(1L).code("1120")
                    .name("Resident Receivables").accountType(AccountType.ASSET)
                    .currentBalance(BigDecimal.ZERO).build();
            Account income = Account.builder().id(2L).code("4100")
                    .name("Maintenance Income").accountType(AccountType.INCOME)
                    .currentBalance(BigDecimal.ZERO).build();

            FiscalYear fy = FiscalYear.builder().id(1L).name("2026-27")
                    .startDate(LocalDate.of(2026, 4, 1))
                    .endDate(LocalDate.of(2027, 3, 31)).build();
            AccountingPeriod period = AccountingPeriod.builder().id(1L)
                    .fiscalYear(fy).build();

            when(accountRepository.findByCode("1120")).thenReturn(Optional.of(receivable));
            when(accountRepository.findByCode("4100")).thenReturn(Optional.of(income));
            when(fiscalYearRepository.findByIsCurrentTrue()).thenReturn(Optional.of(fy));
            when(accountingPeriodRepository.findByFiscalYearAndDate(eq(fy), any(LocalDate.class)))
                    .thenReturn(Optional.of(period));
            when(documentSequenceService.nextNumber(DocumentType.JOURNAL_ENTRY, "2026-27"))
                    .thenReturn("JE-2627-000001");
            when(journalEntryRepository.save(any(JournalEntry.class)))
                    .thenAnswer(inv -> {
                        JournalEntry je = inv.getArgument(0);
                        je.setId(1L);
                        return je;
                    });

            JournalEntryRequest request = JournalEntryRequest.builder()
                    .entryDate(LocalDate.of(2026, 7, 25))
                    .narration("Monthly maintenance billing")
                    .sourceModule(SourceModule.BILLING)
                    .lines(List.of(
                            JournalEntryRequest.LineRequest.builder()
                                    .accountCode("1120")
                                    .debitAmount(new BigDecimal("5000.00"))
                                    .creditAmount(BigDecimal.ZERO)
                                    .build(),
                            JournalEntryRequest.LineRequest.builder()
                                    .accountCode("4100")
                                    .debitAmount(BigDecimal.ZERO)
                                    .creditAmount(new BigDecimal("5000.00"))
                                    .build()
                    ))
                    .build();

            JournalEntry result = accountingEngine.createAndPostJournalEntry(request);

            assertThat(result.getEntryNumber()).isEqualTo("JE-2627-000001");
            assertThat(result.getStatus()).isEqualTo(JournalEntryStatus.POSTED);
            assertThat(result.getTotalDebit()).isEqualByComparingTo("5000.00");
            assertThat(result.getTotalCredit()).isEqualByComparingTo("5000.00");
        }

        @Test
        @DisplayName("should reject unbalanced journal entry")
        void shouldRejectUnbalancedEntry() {
            FiscalYear fy = FiscalYear.builder().id(1L).name("2026-27")
                    .startDate(LocalDate.of(2026, 4, 1))
                    .endDate(LocalDate.of(2027, 3, 31)).build();

            when(fiscalYearRepository.findByIsCurrentTrue()).thenReturn(Optional.of(fy));

            JournalEntryRequest request = JournalEntryRequest.builder()
                    .entryDate(LocalDate.of(2026, 7, 25))
                    .narration("Bad entry")
                    .sourceModule(SourceModule.MANUAL)
                    .lines(List.of(
                            JournalEntryRequest.LineRequest.builder()
                                    .accountCode("1120")
                                    .debitAmount(new BigDecimal("5000.00"))
                                    .creditAmount(BigDecimal.ZERO)
                                    .build(),
                            JournalEntryRequest.LineRequest.builder()
                                    .accountCode("4100")
                                    .debitAmount(BigDecimal.ZERO)
                                    .creditAmount(new BigDecimal("4000.00"))
                                    .build()
                    ))
                    .build();

            assertThatThrownBy(() -> accountingEngine.createAndPostJournalEntry(request))
                    .isInstanceOf(UnbalancedJournalEntryException.class);
        }
    }
}

package com.manacommunity.api.finance.personal;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.finance.personal.dto.PersonalTransactionDto;
import com.manacommunity.api.finance.personal.entity.PersonalTransaction;
import com.manacommunity.api.finance.personal.repository.*;
import com.manacommunity.api.finance.personal.service.impl.PersonalFinanceServiceImpl;
import com.manacommunity.api.user.model.AppUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PersonalFinancePrivacyTest {

    @Mock private PersonalAccountRepository accountRepository;
    @Mock private PersonalCategoryRepository categoryRepository;
    @Mock private PersonalTransactionRepository transactionRepository;
    @Mock private PersonalBudgetRepository budgetRepository;
    @Mock private PersonalBillRepository billRepository;
    @Mock private PersonalRecurringRepository recurringRepository;
    @Mock private PersonalInstallmentRepository installmentRepository;
    @Mock private PersonalGoalRepository goalRepository;

    private PersonalFinanceServiceImpl financeService;

    private AppUser user1;
    private AppUser user2;

    @BeforeEach
    void setUp() {
        financeService = new PersonalFinanceServiceImpl(
                accountRepository,
                categoryRepository,
                transactionRepository,
                budgetRepository,
                billRepository,
                recurringRepository,
                installmentRepository,
                goalRepository
        );

        user1 = AppUser.builder().id(101L).phone("1111111111").fullName("Alice").build();
        user2 = AppUser.builder().id(202L).phone("2222222222").fullName("Bob").build();
    }

    @Test
    @DisplayName("User cannot access another user's transaction even if UUID is known")
    void cannotAccessOtherUserTransactionById() {
        String knownTxnId = "txn-uuid-12345";

        // Repository returns empty when querying for user2 with user1's txnId
        when(transactionRepository.findByIdAndUserId(knownTxnId, 202L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> financeService.getTransaction(knownTxnId, user2))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Transaction not found: " + knownTxnId);

        verify(transactionRepository).findByIdAndUserId(knownTxnId, 202L);
        verify(transactionRepository, never()).findById(knownTxnId);
    }

    @Test
    @DisplayName("User can access their own transaction by ID")
    void canAccessOwnTransactionById() {
        String txnId = "txn-uuid-12345";
        PersonalTransaction txn = PersonalTransaction.builder()
                .id(txnId)
                .user(user1)
                .type("EXPENSE")
                .amount(new BigDecimal("500.00"))
                .currency("₹")
                .accountId("acc-1")
                .description("Grocery shopping")
                .transactionDate(LocalDate.now())
                .build();

        when(transactionRepository.findByIdAndUserId(txnId, 101L)).thenReturn(Optional.of(txn));

        PersonalTransactionDto dto = financeService.getTransaction(txnId, user1);

        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(txnId);
        assertThat(dto.getAmount()).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(dto.getDescription()).isEqualTo("Grocery shopping");
    }

    @Test
    @DisplayName("User cannot delete another user's transaction")
    void cannotDeleteOtherUserTransaction() {
        String knownTxnId = "txn-uuid-99999";

        when(transactionRepository.findByIdAndUserId(knownTxnId, 202L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> financeService.deleteTransaction(knownTxnId, user2))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(transactionRepository, never()).delete(any(PersonalTransaction.class));
    }
}

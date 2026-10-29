package com.manacommunity.api.finance.common;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Common financial transaction abstraction shared by both
 * Community Finance (society ledgers, vouchers, maintenance) and
 * Personal Finance (resident private transactions, budgets, goals).
 *
 * Enforces architectural separation:
 * Community Finance != Personal Finance
 * while unifying core transaction contracts and auditability.
 */
public interface FinancialTransaction {
    String getTransactionId();
    BigDecimal getAmount();
    String getCurrency();
    String getDescription();
    String getCategory();
    LocalDate getTransactionDate();
    String getPaymentMethod();
    String getReceiptUrl();
    boolean isCommunityTransaction();
    boolean isPersonalTransaction();
    String getSourceModule();
}

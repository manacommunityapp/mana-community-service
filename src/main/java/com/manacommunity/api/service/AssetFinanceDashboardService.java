package com.manacommunity.api.service;

import com.manacommunity.api.dto.AssetFinanceDashboardResponse;
import com.manacommunity.api.model.InvoiceStatus;
import com.manacommunity.api.model.ItemStatus;
import com.manacommunity.api.model.PaymentStatus;
import com.manacommunity.api.model.ProcurementStatus;
import com.manacommunity.api.repository.BudgetAllocationRepository;
import com.manacommunity.api.repository.InventoryItemRepository;
import com.manacommunity.api.repository.PurchaseRequestRepository;
import com.manacommunity.api.repository.VendorInvoiceRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class AssetFinanceDashboardService {

    private final InventoryItemRepository inventoryItemRepository;
    private final VendorInvoiceRepository vendorInvoiceRepository;
    private final BudgetAllocationRepository budgetAllocationRepository;
    private final PurchaseRequestRepository purchaseRequestRepository;

    public AssetFinanceDashboardService(InventoryItemRepository inventoryItemRepository,
                                        VendorInvoiceRepository vendorInvoiceRepository,
                                        BudgetAllocationRepository budgetAllocationRepository,
                                        PurchaseRequestRepository purchaseRequestRepository) {
        this.inventoryItemRepository = inventoryItemRepository;
        this.vendorInvoiceRepository = vendorInvoiceRepository;
        this.budgetAllocationRepository = budgetAllocationRepository;
        this.purchaseRequestRepository = purchaseRequestRepository;
    }

    public AssetFinanceDashboardResponse getDashboard(String financialYear) {
        BigDecimal annualBudget = budgetAllocationRepository.findByFinancialYearOrderByCategoryAsc(financialYear)
                .stream()
                .map(b -> b.getAllocatedAmount() != null ? b.getAllocatedAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal budgetSpent = budgetAllocationRepository.findByFinancialYearOrderByCategoryAsc(financialYear)
                .stream()
                .map(b -> b.getSpentAmount() != null ? b.getSpentAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return AssetFinanceDashboardResponse.builder()
                .inventory(AssetFinanceDashboardResponse.InventorySummary.builder()
                        .totalAssets(inventoryItemRepository.count())
                        .available(inventoryItemRepository.countByStatus(ItemStatus.AVAILABLE))
                        .borrowed(inventoryItemRepository.countByStatus(ItemStatus.BORROWED))
                        .maintenance(inventoryItemRepository.countByStatus(ItemStatus.MAINTENANCE))
                        .lost(inventoryItemRepository.countByStatus(ItemStatus.LOST))
                        .disposed(inventoryItemRepository.countByStatus(ItemStatus.DISPOSED))
                        .build())
                .finance(AssetFinanceDashboardResponse.FinanceSummary.builder()
                        .annualBudget(annualBudget)
                        .spent(budgetSpent.max(vendorInvoiceRepository.sumApprovedTotalAmount()))
                        .available(annualBudget.subtract(budgetSpent))
                        .invoicesPending(vendorInvoiceRepository.countByStatus(InvoiceStatus.PENDING))
                        .outstandingVendorBills(vendorInvoiceRepository.countByPaymentStatus(PaymentStatus.PENDING))
                        .pendingPayments(vendorInvoiceRepository.sumPendingPaymentAmount())
                        .build())
                .procurement(AssetFinanceDashboardResponse.ProcurementSummary.builder()
                        .purchaseRequests(purchaseRequestRepository.count())
                        .pendingApproval(purchaseRequestRepository.countByStatus(ProcurementStatus.REQUESTED))
                        .purchaseOrdered(purchaseRequestRepository.countByStatus(ProcurementStatus.PURCHASE_ORDERED))
                        .goodsReceived(purchaseRequestRepository.countByStatus(ProcurementStatus.GOODS_RECEIVED))
                        .build())
                .build();
    }
}

package com.manacommunity.api.service;

import com.manacommunity.api.dto.InvoiceDto;
import com.manacommunity.api.dto.LineItemDto;
import com.manacommunity.api.dto.PaymentRequest;
import com.manacommunity.api.model.*;
import com.manacommunity.api.repository.BudgetAllocationRepository;
import com.manacommunity.api.repository.InventoryItemRepository;
import com.manacommunity.api.repository.InvoiceLineItemRepository;
import com.manacommunity.api.repository.PurchaseRequestRepository;
import com.manacommunity.api.repository.VendorRepository;
import com.manacommunity.api.repository.VendorInvoiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class InvoiceService {

    @Autowired
    private VendorInvoiceRepository vendorInvoiceRepository;

    @Autowired
    private InvoiceLineItemRepository invoiceLineItemRepository;

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    @Autowired
    private VendorRepository vendorRepository;

    @Autowired
    private BudgetAllocationRepository budgetAllocationRepository;

    @Autowired
    private PurchaseRequestRepository purchaseRequestRepository;

    public List<VendorInvoice> getAllInvoices(Long communityId) {
        return vendorInvoiceRepository.findByCommunityIdOrderByCreatedAtDesc(communityId);
    }

    public Optional<VendorInvoice> getInvoiceById(Long id, Long communityId) {
        return vendorInvoiceRepository.findByIdAndCommunityId(id, communityId);
    }

    @Transactional
    public VendorInvoice createInvoiceFromOcr(InvoiceDto dto, Long communityId) {
        Vendor vendor = dto.getVendorId() == null
                ? null
                : vendorRepository.findById(dto.getVendorId()).orElse(null);
        ExpenseCategory invoiceCategory = parseCategory(dto.getExpenseCategory());

        VendorInvoice invoice = VendorInvoice.builder()
                .communityId(communityId)
                .invoiceNumber(dto.getInvoiceNumber())
                .invoiceDate(dto.getInvoiceDate())
                .vendor(vendor)
                .vendorName(vendor != null ? vendor.getName() : dto.getVendorName())
                .vendorGstNumber(dto.getVendorGstNumber())
                .totalAmount(dto.getTotalAmount())
                .taxableAmount(dto.getTaxableAmount())
                .cgst(defaultMoney(dto.getCgst()))
                .sgst(defaultMoney(dto.getSgst()))
                .igst(defaultMoney(dto.getIgst()))
                .expenseCategory(invoiceCategory)
                .status(InvoiceStatus.PENDING)
                .paymentStatus(PaymentStatus.PENDING)
                .uploadedBy(dto.getUploadedBy())
                .receiptUrl(dto.getReceiptUrl())
                .upiId(dto.getUpiId())
                .bankDetails(dto.getBankDetails())
                .dueDate(dto.getDueDate())
                .purchaseRequestId(dto.getPurchaseRequestId())
                .purchaseOrderNumber(dto.getPurchaseOrderNumber())
                .ocrRawMetadata(dto.getOcrRawMetadata())
                .build();

        VendorInvoice savedInvoice = vendorInvoiceRepository.save(invoice);

        if (dto.getLineItems() != null) {
            for (LineItemDto itemDto : dto.getLineItems()) {
                InvoiceLineItem lineItem = InvoiceLineItem.builder()
                        .invoice(savedInvoice)
                        .description(itemDto.getDescription())
                        .quantity(itemDto.getQuantity())
                        .unitPrice(itemDto.getUnitPrice())
                        .totalPrice(itemDto.getTotalPrice())
                        .cgst(itemDto.getCgst())
                        .sgst(itemDto.getSgst())
                        .igst(itemDto.getIgst())
                        .expenseCategory(itemDto.getExpenseCategory())
                        .build();

                // If CapEx_Asset (New Asset Purchase), provision a new InventoryItem row
                boolean createAsset = itemDto.getExpenseCategory() == ExpenseCategory.CapEx_Asset
                        || Boolean.TRUE.equals(itemDto.getCreateInventoryItem());
                if (createAsset) {
                    InventoryItem newItem = InventoryItem.builder()
                            .name(itemDto.getDescription())
                            .category(itemDto.getAssetCategory() != null ? itemDto.getAssetCategory() : "CapEx_Asset")
                            .location(itemDto.getLocation())
                            .serialNumber(itemDto.getSerialNumber())
                            .vendorName(savedInvoice.getVendorName())
                            .status(ItemStatus.AVAILABLE)
                            .originalCost(itemDto.getUnitPrice())
                            .tco(itemDto.getUnitPrice())
                            .currentValue(itemDto.getUnitPrice())
                            .qrCodeId(UUID.randomUUID().toString().substring(0, 8))
                            .build();

                    InventoryItem savedItem = inventoryItemRepository.save(newItem);
                    lineItem.setItem(savedItem);
                } else if (dto.getAssetId() != null) {
                    // Link to existing asset (e.g. for maintenance or consumables)
                    inventoryItemRepository.findById(dto.getAssetId()).ifPresent(lineItem::setItem);
                }

                invoiceLineItemRepository.save(lineItem);
                savedInvoice.getLineItems().add(lineItem);
            }
        }

        if (dto.getPurchaseRequestId() != null) {
            purchaseRequestRepository.findById(dto.getPurchaseRequestId()).ifPresent(request -> {
                request.setStatus(ProcurementStatus.INVOICED);
                request.setPurchaseOrderNumber(dto.getPurchaseOrderNumber());
                purchaseRequestRepository.save(request);
            });
        }

        return vendorInvoiceRepository.save(savedInvoice);
    }

    @Transactional
    public Optional<VendorInvoice> approveInvoice(Long id, Long communityId, String notes) {
        return vendorInvoiceRepository.findByIdAndCommunityId(id, communityId).map(invoice -> {
            invoice.setStatus(InvoiceStatus.APPROVED);
            invoice.setApprovalNotes(notes);
            invoice.setApprovedAt(LocalDateTime.now());

            // Audit & adjust lifecycle TCO for any linked assets
            for (InvoiceLineItem lineItem : invoice.getLineItems()) {
                if (lineItem.getItem() != null) {
                    InventoryItem asset = lineItem.getItem();
                    BigDecimal currentTco = asset.getTco() != null ? asset.getTco() : BigDecimal.ZERO;
                    if (lineItem.getExpenseCategory() != ExpenseCategory.CapEx_Asset) {
                        asset.setTco(currentTco.add(lineItem.getTotalPrice()));
                    }
                    inventoryItemRepository.save(asset);
                }
            }

            if (invoice.getExpenseCategory() != null && invoice.getCommunityId() != null) {
                budgetAllocationRepository
                        .findByCommunityIdAndFinancialYearAndCategory(invoice.getCommunityId(), resolveFinancialYear(invoice), invoice.getExpenseCategory())
                        .ifPresent(budget -> {
                            budget.setSpentAmount(budget.getSpentAmount().add(invoice.getTotalAmount()));
                            budgetAllocationRepository.save(budget);
                        });
            }

            if (invoice.getPurchaseRequestId() != null) {
                purchaseRequestRepository.findById(invoice.getPurchaseRequestId()).ifPresent(request -> {
                    request.setStatus(ProcurementStatus.INVENTORY_CREATED);
                    purchaseRequestRepository.save(request);
                });
            }

            return vendorInvoiceRepository.save(invoice);
        });
    }

    @Transactional
    public Optional<VendorInvoice> rejectInvoice(Long id, Long communityId, String notes) {
        return vendorInvoiceRepository.findByIdAndCommunityId(id, communityId).map(invoice -> {
            invoice.setStatus(InvoiceStatus.REJECTED);
            invoice.setApprovalNotes(notes);
            invoice.setRejectedAt(LocalDateTime.now());
            return vendorInvoiceRepository.save(invoice);
        });
    }

    @Transactional
    public Optional<VendorInvoice> markPaid(Long id, Long communityId, PaymentRequest request) {
        return vendorInvoiceRepository.findByIdAndCommunityId(id, communityId).map(invoice -> {
            invoice.setStatus(InvoiceStatus.PAID);
            invoice.setPaymentStatus(PaymentStatus.PAID);
            invoice.setPaymentMode(request.getPaymentMode());
            invoice.setPaymentReference(request.getPaymentReference());
            invoice.setPaidAt(request.getPaidAt() != null ? request.getPaidAt() : LocalDateTime.now());
            return vendorInvoiceRepository.save(invoice);
        });
    }

    private ExpenseCategory parseCategory(String category) {
        if (category == null || category.isBlank()) {
            return null;
        }
        return ExpenseCategory.valueOf(category);
    }

    private BigDecimal defaultMoney(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private String resolveFinancialYear(VendorInvoice invoice) {
        String invoiceDate = invoice.getInvoiceDate();
        if (invoiceDate != null && invoiceDate.length() >= 4) {
            return invoiceDate.substring(0, 4);
        }
        return String.valueOf(java.time.LocalDate.now().getYear());
    }
}

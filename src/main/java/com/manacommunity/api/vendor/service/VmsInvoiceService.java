package com.manacommunity.api.vendor.service;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.vendor.dto.InvoiceRequest;
import com.manacommunity.api.vendor.dto.InvoiceResponse;
import com.manacommunity.api.vendor.entity.*;
import com.manacommunity.api.vendor.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VmsInvoiceService {
    private final VmsInvoiceRepository invoiceRepo;
    private final VmsInvoiceItemRepository itemRepo;
    private final VmsVendorRepository vendorRepo;
    private final VmsContractRepository contractRepo;

    @Transactional(readOnly = true)
    public Page<InvoiceResponse> listCommunity(Long communityId, Long vendorId, String status, Pageable pageable) {
        Page<VmsInvoice> page;
        if (vendorId != null) page = invoiceRepo.findByVendorIdAndCommunityId(vendorId, communityId, pageable);
        else if (status != null && !status.isBlank()) page = invoiceRepo.findByCommunityIdAndStatus(communityId, VmsInvoice.InvoiceStatus.valueOf(status.toUpperCase()), pageable);
        else page = invoiceRepo.findByCommunityId(communityId, pageable);
        return page.map(this::response);
    }

    @Transactional(readOnly = true)
    public Page<InvoiceResponse> listMine(AppUser user, Pageable pageable) {
        VmsVendor vendor = myVendor(user);
        return invoiceRepo.findByVendorIdAndCommunityId(vendor.getId(), user.getCommunity().getId(), pageable).map(this::response);
    }

    @Transactional(readOnly = true)
    public InvoiceResponse get(Long id, Long communityId) {
        VmsInvoice invoice = invoiceRepo.findByIdAndCommunityId(id, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", id));
        return response(invoice);
    }

    @Transactional
    public InvoiceResponse create(InvoiceRequest req, AppUser user, Community community, boolean manager) {
        VmsVendor vendor = vendorRepo.findByIdAndCommunityId(req.getVendorId(), community.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Vendor", req.getVendorId()));
        if ((vendor.getUser() == null || !vendor.getUser().getId().equals(user.getId())) && !manager)
            throw new org.springframework.security.access.AccessDeniedException("You may only create invoices for your vendor account");
        if (req.getItems() == null || req.getItems().isEmpty()) throw new IllegalArgumentException("An invoice must contain at least one item");
        VmsContract contract = null;
        if (req.getContractId() != null) {
            contract = contractRepo.findByIdAndCommunityId(req.getContractId(), community.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Contract", req.getContractId()));
        }
        VmsInvoice invoice = invoiceRepo.save(VmsInvoice.builder().invoiceNumber("INV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .vendor(vendor).community(community).createdByUser(user).invoiceDate(LocalDate.now()).dueDate(req.getDueDate())
                .contract(contract).notes(req.getNotes()).status(VmsInvoice.InvoiceStatus.DRAFT).build());
        BigDecimal subtotal = BigDecimal.ZERO, calculatedTax = BigDecimal.ZERO, calculatedDiscount = BigDecimal.ZERO;
        int order = 0;
        for (InvoiceRequest.InvoiceItemRequest item : req.getItems()) {
            BigDecimal qty = item.getQuantity() == null ? BigDecimal.ONE : item.getQuantity();
            BigDecimal price = item.getUnitPrice() == null ? BigDecimal.ZERO : item.getUnitPrice();
            BigDecimal base = qty.multiply(price).setScale(2, RoundingMode.HALF_UP);
            BigDecimal discount = item.getDiscountPercent() == null ? BigDecimal.ZERO : base.multiply(item.getDiscountPercent()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            BigDecimal taxable = base.subtract(discount);
            BigDecimal tax = item.getTaxPercent() == null ? BigDecimal.ZERO : taxable.multiply(item.getTaxPercent()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            BigDecimal total = taxable.add(tax);
            subtotal = subtotal.add(base); calculatedDiscount = calculatedDiscount.add(discount); calculatedTax = calculatedTax.add(tax);
            itemRepo.save(VmsInvoiceItem.builder().invoice(invoice).description(item.getDescription()).quantity(qty).unit(item.getUnit())
                    .unitPrice(price).taxPercent(item.getTaxPercent()).discountPercent(item.getDiscountPercent()).total(total).sortOrder(order++).build());
        }
        BigDecimal discount = req.getDiscountAmount() == null ? calculatedDiscount : req.getDiscountAmount();
        BigDecimal tax = req.getTaxAmount() == null ? calculatedTax : req.getTaxAmount();
        invoice.setSubtotal(subtotal); invoice.setDiscountAmount(discount); invoice.setTaxAmount(tax);
        invoice.setTotalAmount(subtotal.subtract(discount).add(tax).max(BigDecimal.ZERO));
        return response(invoiceRepo.save(invoice));
    }

    @Transactional
    public InvoiceResponse updateStatus(Long id, String status, Long communityId) {
        VmsInvoice invoice = invoiceRepo.findByIdAndCommunityId(id, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", id));
        VmsInvoice.InvoiceStatus next = VmsInvoice.InvoiceStatus.valueOf(status.toUpperCase());
        if (invoice.getStatus() == VmsInvoice.InvoiceStatus.PAID && next != VmsInvoice.InvoiceStatus.PAID)
            throw new IllegalStateException("A paid invoice cannot be reopened through a status update");
        invoice.setStatus(next);
        if (next == VmsInvoice.InvoiceStatus.PAID) invoice.setPaidAmount(invoice.getTotalAmount());
        return response(invoiceRepo.save(invoice));
    }

    @Transactional
    public void send(Long id, Long communityId) {
        VmsInvoice invoice = invoiceRepo.findByIdAndCommunityId(id, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", id));
        if (invoice.getStatus() == VmsInvoice.InvoiceStatus.DRAFT) { invoice.setStatus(VmsInvoice.InvoiceStatus.SENT); invoiceRepo.save(invoice); }
    }

    private InvoiceResponse response(VmsInvoice invoice) {
        List<InvoiceResponse.InvoiceItemResponse> items = itemRepo.findByInvoiceIdOrderBySortOrder(invoice.getId()).stream()
                .map(i -> InvoiceResponse.InvoiceItemResponse.builder().id(i.getId()).description(i.getDescription()).quantity(i.getQuantity())
                        .unit(i.getUnit()).unitPrice(i.getUnitPrice()).taxPercent(i.getTaxPercent()).discountPercent(i.getDiscountPercent()).total(i.getTotal()).build()).toList();
        BigDecimal balance = invoice.getTotalAmount().subtract(invoice.getPaidAmount() == null ? BigDecimal.ZERO : invoice.getPaidAmount()).max(BigDecimal.ZERO);
        return InvoiceResponse.builder().id(invoice.getId()).invoiceNumber(invoice.getInvoiceNumber())
                .vendor(InvoiceResponse.VendorRef.builder().id(invoice.getVendor().getId()).businessName(invoice.getVendor().getBusinessName()).build())
                .status(invoice.getStatus().name()).issueDate(invoice.getInvoiceDate()).dueDate(invoice.getDueDate())
                .subtotal(invoice.getSubtotal()).taxAmount(invoice.getTaxAmount()).discountAmount(invoice.getDiscountAmount())
                .totalAmount(invoice.getTotalAmount()).paidAmount(invoice.getPaidAmount()).balanceDue(balance).notes(invoice.getNotes())
                .items(items).communityId(invoice.getCommunity().getId()).createdAt(invoice.getCreatedAt()).build();
    }

    private VmsVendor myVendor(AppUser user) {
        return vendorRepo.findByUserIdAndCommunityId(user.getId(), user.getCommunity().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Vendor", "userId", user.getId().toString()));
    }
 }

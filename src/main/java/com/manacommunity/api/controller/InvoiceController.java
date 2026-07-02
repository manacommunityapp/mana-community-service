package com.manacommunity.api.controller;

import com.manacommunity.api.dto.InvoiceDto;
import com.manacommunity.api.dto.PaymentRequest;
import com.manacommunity.api.model.VendorInvoice;
import com.manacommunity.api.service.InvoiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/inventory/expenses", "/api/asset-finance/invoices"})
public class InvoiceController {

    @Autowired
    private InvoiceService invoiceService;

    @GetMapping
    public ResponseEntity<List<VendorInvoice>> getInvoices() {
        return ResponseEntity.ok(invoiceService.getAllInvoices());
    }

    @GetMapping("/{id}")
    public ResponseEntity<VendorInvoice> getInvoiceById(@PathVariable Long id) {
        return invoiceService.getInvoiceById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/upload")
    public ResponseEntity<VendorInvoice> uploadInvoice(@RequestBody InvoiceDto dto) {
        return ResponseEntity.ok(invoiceService.createInvoiceFromOcr(dto));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<VendorInvoice> approveInvoice(@PathVariable Long id, @RequestParam(required = false) String notes) {
        String approvalNotes = notes != null ? notes : "Approved by Treasurer";
        return invoiceService.approveInvoice(id, approvalNotes)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<VendorInvoice> rejectInvoice(@PathVariable Long id, @RequestParam(required = false) String notes) {
        String rejectionNotes = notes != null ? notes : "Rejected by Treasurer";
        return invoiceService.rejectInvoice(id, rejectionNotes)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/pay")
    public ResponseEntity<VendorInvoice> markPaid(@PathVariable Long id, @RequestBody PaymentRequest request) {
        return invoiceService.markPaid(id, request)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}

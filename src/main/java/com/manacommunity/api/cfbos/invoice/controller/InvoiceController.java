package com.manacommunity.api.cfbos.invoice.controller;

import com.manacommunity.api.cfbos.invoice.dto.InvoiceResponse;
import com.manacommunity.api.cfbos.invoice.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController("cfbosInvoiceController")
@RequestMapping("/api/cfbos/invoices")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('Manage Finance') or hasAuthority('Admin') or hasAuthority('Finance Admin')")
public class InvoiceController {

    private final InvoiceService invoiceService;

    @PostMapping("/generate/run/{billingRunId}")
    public ResponseEntity<List<InvoiceResponse>> generateFromRun(@PathVariable Long billingRunId) {
        return ResponseEntity.ok(invoiceService.generateInvoicesFromRun(billingRunId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<InvoiceResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(invoiceService.getInvoiceById(id));
    }

    @GetMapping("/resident/{residentId}")
    public ResponseEntity<List<InvoiceResponse>> getByResident(@PathVariable Long residentId) {
        return ResponseEntity.ok(invoiceService.getInvoicesByResident(residentId));
    }

    @GetMapping
    public ResponseEntity<List<InvoiceResponse>> getAll() {
        return ResponseEntity.ok(invoiceService.getAllInvoices());
    }
}

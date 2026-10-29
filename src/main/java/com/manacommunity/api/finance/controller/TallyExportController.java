package com.manacommunity.api.finance.controller;

import com.manacommunity.api.model.Invoice;
import com.manacommunity.api.repository.InvoiceRepository;
import com.manacommunity.api.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/finance/tally-export")
@RequiredArgsConstructor
public class TallyExportController {

    private static final DateTimeFormatter TALLY_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final InvoiceRepository invoiceRepo;

    @PostMapping("/sales-vouchers")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<String> exportSalesVouchers(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam String fromDate,
            @RequestParam String toDate) {
        Long communityId = principal.getCommunityId();
        LocalDate from = LocalDate.parse(fromDate);
        LocalDate to = LocalDate.parse(toDate);

        List<Invoice> invoices = invoiceRepo.findByCommunityIdOrderByGeneratedAtDesc(
                communityId, PageRequest.of(0, 5000)).getContent()
                .stream()
                .filter(inv -> {
                    LocalDate genDate = inv.getGeneratedAt().toLocalDate();
                    return !genDate.isBefore(from) && !genDate.isAfter(to);
                })
                .toList();

        StringBuilder xml = buildSalesVoucherXml(invoices, communityId);

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=Sales_Vouchers_" + fromDate + "_" + toDate + ".xml");
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_XML)
                .body(xml.toString());
    }

    @PostMapping("/receipt-vouchers")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<String> exportReceiptVouchers(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam String fromDate,
            @RequestParam String toDate) {
        Long communityId = principal.getCommunityId();
        LocalDate from = LocalDate.parse(fromDate);
        LocalDate to = LocalDate.parse(toDate);

        List<Invoice> paidInvoices = invoiceRepo.findByCommunityIdOrderByGeneratedAtDesc(
                communityId, PageRequest.of(0, 5000)).getContent()
                .stream()
                .filter(inv -> "PAID".equals(inv.getStatus()) && inv.getPaidAt() != null)
                .filter(inv -> {
                    LocalDate paidDate = inv.getPaidAt().toLocalDate();
                    return !paidDate.isBefore(from) && !paidDate.isAfter(to);
                })
                .toList();

        StringBuilder xml = buildReceiptVoucherXml(paidInvoices, communityId);

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=Receipt_Vouchers_" + fromDate + "_" + toDate + ".xml");
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_XML)
                .body(xml.toString());
    }

    private StringBuilder buildSalesVoucherXml(List<Invoice> invoices, Long communityId) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<ENVELOPE>\n");
        sb.append("  <HEADER><TALLYREQUEST>Import Data</TALLYREQUEST></HEADER>\n");
        sb.append("  <BODY><IMPORTDATA><REQUESTDESC><REPORTNAME>Vouchers</REPORTNAME></REQUESTDESC>\n");
        sb.append("    <REQUESTDATA>\n");
        for (Invoice inv : invoices) {
            String dateStr = inv.getGeneratedAt().toLocalDate().format(TALLY_DATE);
            String residentName = inv.getResident() != null ? inv.getResident().getFullName() : "Resident";
            String flatNo = inv.getResident() != null ? inv.getResident().getFlatNo() : "";
            String ledgerName = flatNo + " - " + residentName;
            String guid = inv.getId() + "-" + communityId;
            sb.append("      <TALLYMESSAGE xmlns:UDF=\"TallyUDF\">\n");
            sb.append("        <VOUCHER VCHTYPE=\"Sales\" ACTION=\"Create\">\n");
            sb.append("          <DATE>").append(dateStr).append("</DATE>\n");
            sb.append("          <GUID>").append(guid).append("</GUID>\n");
            sb.append("          <VOUCHERNUMBER>").append(inv.getInvoiceNumber()).append("</VOUCHERNUMBER>\n");
            sb.append("          <PARTYLEDGERNAME>").append(escapeXml(ledgerName)).append("</PARTYLEDGERNAME>\n");
            // Debit: flat debtor account
            sb.append("          <ALLLEDGERENTRIES.LIST>\n");
            sb.append("            <LEDGERNAME>").append(escapeXml(ledgerName)).append("</LEDGERNAME>\n");
            sb.append("            <ISDEEMEDPOSITIVE>Yes</ISDEEMEDPOSITIVE>\n");
            sb.append("            <AMOUNT>-").append(inv.getTotalAmount()).append("</AMOUNT>\n");
            sb.append("            <BILLALLOCATIONS.LIST>\n");
            sb.append("              <NAME>").append(inv.getInvoiceNumber()).append("</NAME>\n");
            sb.append("              <BILLTYPE>New Ref</BILLTYPE>\n");
            sb.append("              <AMOUNT>-").append(inv.getTotalAmount()).append("</AMOUNT>\n");
            sb.append("            </BILLALLOCATIONS.LIST>\n");
            sb.append("          </ALLLEDGERENTRIES.LIST>\n");
            // Credit: maintenance income
            sb.append("          <ALLLEDGERENTRIES.LIST>\n");
            sb.append("            <LEDGERNAME>Society Maintenance Charges</LEDGERNAME>\n");
            sb.append("            <ISDEEMEDPOSITIVE>No</ISDEEMEDPOSITIVE>\n");
            sb.append("            <AMOUNT>").append(inv.getTaxableAmount()).append("</AMOUNT>\n");
            sb.append("          </ALLLEDGERENTRIES.LIST>\n");
            // Credit: CGST
            sb.append("          <ALLLEDGERENTRIES.LIST>\n");
            sb.append("            <LEDGERNAME>Output CGST (9%)</LEDGERNAME>\n");
            sb.append("            <ISDEEMEDPOSITIVE>No</ISDEEMEDPOSITIVE>\n");
            sb.append("            <AMOUNT>").append(inv.getCgst()).append("</AMOUNT>\n");
            sb.append("          </ALLLEDGERENTRIES.LIST>\n");
            // Credit: SGST
            sb.append("          <ALLLEDGERENTRIES.LIST>\n");
            sb.append("            <LEDGERNAME>Output SGST (9%)</LEDGERNAME>\n");
            sb.append("            <ISDEEMEDPOSITIVE>No</ISDEEMEDPOSITIVE>\n");
            sb.append("            <AMOUNT>").append(inv.getSgst()).append("</AMOUNT>\n");
            sb.append("          </ALLLEDGERENTRIES.LIST>\n");
            sb.append("        </VOUCHER>\n");
            sb.append("      </TALLYMESSAGE>\n");
        }
        sb.append("    </REQUESTDATA>\n");
        sb.append("  </IMPORTDATA></BODY>\n");
        sb.append("</ENVELOPE>");
        return sb;
    }

    private StringBuilder buildReceiptVoucherXml(List<Invoice> invoices, Long communityId) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<ENVELOPE>\n");
        sb.append("  <HEADER><TALLYREQUEST>Import Data</TALLYREQUEST></HEADER>\n");
        sb.append("  <BODY><IMPORTDATA><REQUESTDESC><REPORTNAME>Vouchers</REPORTNAME></REQUESTDESC>\n");
        sb.append("    <REQUESTDATA>\n");
        for (Invoice inv : invoices) {
            String dateStr = inv.getPaidAt().toLocalDate().format(TALLY_DATE);
            String residentName = inv.getResident() != null ? inv.getResident().getFullName() : "Resident";
            String flatNo = inv.getResident() != null ? inv.getResident().getFlatNo() : "";
            String ledgerName = flatNo + " - " + residentName;
            String guid = "REC-" + inv.getId() + "-" + communityId;
            sb.append("      <TALLYMESSAGE xmlns:UDF=\"TallyUDF\">\n");
            sb.append("        <VOUCHER VCHTYPE=\"Receipt\" ACTION=\"Create\">\n");
            sb.append("          <DATE>").append(dateStr).append("</DATE>\n");
            sb.append("          <GUID>").append(guid).append("</GUID>\n");
            sb.append("          <VOUCHERNUMBER>REC-").append(inv.getInvoiceNumber()).append("</VOUCHERNUMBER>\n");
            sb.append("          <PARTYLEDGERNAME>").append(escapeXml(ledgerName)).append("</PARTYLEDGERNAME>\n");
            // Debit: clearing account
            sb.append("          <ALLLEDGERENTRIES.LIST>\n");
            sb.append("            <LEDGERNAME>MyGate Inward Clearing Account</LEDGERNAME>\n");
            sb.append("            <ISDEEMEDPOSITIVE>Yes</ISDEEMEDPOSITIVE>\n");
            sb.append("            <AMOUNT>-").append(inv.getTotalAmount()).append("</AMOUNT>\n");
            sb.append("          </ALLLEDGERENTRIES.LIST>\n");
            // Credit: flat debtor (Agst Ref)
            sb.append("          <ALLLEDGERENTRIES.LIST>\n");
            sb.append("            <LEDGERNAME>").append(escapeXml(ledgerName)).append("</LEDGERNAME>\n");
            sb.append("            <ISDEEMEDPOSITIVE>No</ISDEEMEDPOSITIVE>\n");
            sb.append("            <AMOUNT>").append(inv.getTotalAmount()).append("</AMOUNT>\n");
            sb.append("            <BILLALLOCATIONS.LIST>\n");
            sb.append("              <NAME>").append(inv.getInvoiceNumber()).append("</NAME>\n");
            sb.append("              <BILLTYPE>Agst Ref</BILLTYPE>\n");
            sb.append("              <AMOUNT>").append(inv.getTotalAmount()).append("</AMOUNT>\n");
            sb.append("            </BILLALLOCATIONS.LIST>\n");
            sb.append("          </ALLLEDGERENTRIES.LIST>\n");
            sb.append("        </VOUCHER>\n");
            sb.append("      </TALLYMESSAGE>\n");
        }
        sb.append("    </REQUESTDATA>\n");
        sb.append("  </IMPORTDATA></BODY>\n");
        sb.append("</ENVELOPE>");
        return sb;
    }

    private String escapeXml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }
}

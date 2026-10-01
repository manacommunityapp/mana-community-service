package com.manacommunity.api.cfbos.integrations.controller;

import com.manacommunity.api.cfbos.integrations.service.QuickBooksExportService;
import com.manacommunity.api.cfbos.integrations.service.TallyXmlExportService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/finance/export")
@RequiredArgsConstructor
public class AccountingExportController {

    private final TallyXmlExportService tallyExportService;
    private final QuickBooksExportService quickBooksExportService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping(value = "/tally-xml", produces = MediaType.APPLICATION_XML_VALUE)
    @PreAuthorize("hasAuthority('Export Accounting') or hasAuthority('Admin')")
    public ResponseEntity<String> exportTally(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : 1L;
        String xml = tallyExportService.exportReceiptVouchers(communityId, startDate, endDate);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=tally_import.xml")
                .body(xml);
    }

    @GetMapping(value = "/quickbooks", produces = MediaType.TEXT_PLAIN_VALUE)
    @PreAuthorize("hasAuthority('Export Accounting') or hasAuthority('Admin')")
    public ResponseEntity<String> exportQuickBooks(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : 1L;
        String iif = quickBooksExportService.exportIifVouchers(communityId, startDate, endDate);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=quickbooks_import.iif")
                .body(iif);
    }
}

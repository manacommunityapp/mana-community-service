package com.manacommunity.api.controller;

import com.manacommunity.api.notification.dto.SmsDashboardResponse;
import com.manacommunity.api.notification.dto.SmsMessageResponse;
import com.manacommunity.api.notification.enums.SmsStatus;
import com.manacommunity.api.notification.provider.SmsProvider;
import com.manacommunity.api.notification.repository.SmsCostRecordRepository;
import com.manacommunity.api.notification.repository.SmsMessageRepository;
import com.manacommunity.api.notification.service.SmsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/admin/sms")
@PreAuthorize("hasAuthority('View Admin')")
@RequiredArgsConstructor
public class SmsAdminController {

    private final SmsMessageRepository smsMessageRepo;
    private final SmsCostRecordRepository costRecordRepo;
    private final SmsService smsService;
    private final SmsProvider smsProvider;

    @GetMapping("/dashboard")
    public ResponseEntity<SmsDashboardResponse> dashboard() {
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);

        SmsDashboardResponse response = SmsDashboardResponse.builder()
                .totalSent(smsMessageRepo.countByStatus(SmsStatus.SENT)
                        + smsMessageRepo.countByStatus(SmsStatus.DELIVERED))
                .totalDelivered(smsMessageRepo.countByStatus(SmsStatus.DELIVERED))
                .totalFailed(smsMessageRepo.countByStatus(SmsStatus.FAILED))
                .queued(smsMessageRepo.countByStatus(SmsStatus.QUEUED)
                        + smsMessageRepo.countByStatus(SmsStatus.SENDING))
                .dlq(smsMessageRepo.countByStatus(SmsStatus.DLQ))
                .retrying(smsMessageRepo.countByStatus(SmsStatus.RETRYING))
                .costToday(costRecordRepo.sumCostByDate(today))
                .costThisMonth(costRecordRepo.sumCostBetween(monthStart, today))
                .providerHealthy(smsProvider.isHealthy())
                .providerName(smsProvider.getProviderName())
                .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/messages")
    public ResponseEntity<Page<com.manacommunity.api.notification.entity.SmsMessage>> listMessages(
            @RequestParam(required = false) SmsStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        if (status != null) {
            return ResponseEntity.ok(smsMessageRepo.findByStatus(status, pageable));
        }
        return ResponseEntity.ok(smsMessageRepo.findAll(pageable));
    }

    @PostMapping("/messages/{id}/retry")
    public ResponseEntity<SmsMessageResponse> retry(@PathVariable Long id) {
        return ResponseEntity.ok(smsService.retry(id));
    }

    @PostMapping("/messages/{id}/cancel")
    public ResponseEntity<Void> cancel(@PathVariable Long id) {
        smsService.cancel(id);
        return ResponseEntity.noContent().build();
    }
}

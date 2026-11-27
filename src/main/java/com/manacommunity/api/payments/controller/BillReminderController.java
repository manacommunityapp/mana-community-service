package com.manacommunity.api.payments.controller;

import com.manacommunity.api.payments.entity.BillReminder;
import com.manacommunity.api.payments.service.BillReminderService;
import com.manacommunity.api.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/payments/bill-reminders")
@RequiredArgsConstructor
public class BillReminderController {
    private final BillReminderService service;

    @GetMapping
    public List<BillReminder> list(@AuthenticationPrincipal UserPrincipal principal) {
        return service.listAll(principal.getCommunityId());
    }

    @GetMapping("/my")
    public List<BillReminder> mine(@AuthenticationPrincipal UserPrincipal principal) {
        return service.listByUser(principal.getCommunityId(), principal.getUserId());
    }

    @PostMapping
    public BillReminder create(@AuthenticationPrincipal UserPrincipal principal,
                                @RequestBody BillReminder reminder) {
        reminder.setCommunityId(principal.getCommunityId());
        reminder.setUserId(principal.getUserId());
        return service.create(reminder);
    }

    @PutMapping("/{id}/paid")
    public BillReminder markPaid(@AuthenticationPrincipal UserPrincipal principal,
                                  @PathVariable Long id) {
        return service.markPaid(principal.getCommunityId(), id);
    }
}

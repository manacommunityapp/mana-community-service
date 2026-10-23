package com.manacommunity.api.groupbuy.scheduler;

import com.manacommunity.api.groupbuy.service.GroupDealService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class GroupDealExpireJob {

    private final GroupDealService dealService;

    @Scheduled(fixedDelay = 300_000) // every 5 minutes
    public void expireOverdueDeals() {
        try {
            dealService.expireOverdueDeals();
        } catch (Exception e) {
            log.error("GroupDealExpireJob failed: {}", e.getMessage(), e);
        }
    }
}

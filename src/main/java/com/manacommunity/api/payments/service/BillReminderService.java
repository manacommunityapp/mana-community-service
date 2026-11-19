package com.manacommunity.api.payments.service;

import com.manacommunity.api.payments.entity.BillReminder;
import com.manacommunity.api.payments.repository.BillReminderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BillReminderService {
    private final BillReminderRepository repo;

    public List<BillReminder> listByUser(Long communityId, Long userId) {
        return repo.findByCommunityIdAndUserId(communityId, userId);
    }

    public List<BillReminder> listAll(Long communityId) {
        return repo.findByCommunityId(communityId);
    }

    @Transactional
    public BillReminder create(BillReminder reminder) {
        return repo.save(reminder);
    }

    @Transactional
    public BillReminder markPaid(Long communityId, Long id) {
        BillReminder r = repo.findById(id)
            .filter(x -> x.getCommunityId().equals(communityId))
            .orElseThrow(() -> new IllegalArgumentException("Reminder not found"));
        r.setPaid(true);
        r.setPaidAt(Instant.now());
        return repo.save(r);
    }

    @Scheduled(cron = "0 0 9 * * *")
    @Transactional
    public void sendDueReminders() {
        LocalDate cutoff = LocalDate.now().plusDays(3);
        List<BillReminder> due = repo.findByPaidFalseAndReminderSentFalseAndDueDateLessThanEqual(cutoff);
        for (BillReminder r : due) {
            r.setReminderSent(true);
            r.setReminderSentAt(Instant.now());
            repo.save(r);
        }
    }
}

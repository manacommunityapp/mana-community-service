package com.manacommunity.api.payments.repository;

import com.manacommunity.api.payments.entity.BillReminder;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface BillReminderRepository extends JpaRepository<BillReminder, Long> {
    List<BillReminder> findByCommunityIdAndUserId(Long communityId, Long userId);
    List<BillReminder> findByCommunityId(Long communityId);
    List<BillReminder> findByPaidFalseAndReminderSentFalseAndDueDateLessThanEqual(LocalDate cutoff);
}

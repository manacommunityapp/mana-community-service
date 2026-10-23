package com.manacommunity.api.repository;

import com.manacommunity.api.model.SocietyExpenseRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface SocietyExpenseRecordRepository extends JpaRepository<SocietyExpenseRecord, Long> {

    List<SocietyExpenseRecord> findByCommunityIdOrderByDateDesc(Long communityId);

    List<SocietyExpenseRecord> findByCommunityIdAndCategoryOrderByDateDesc(Long communityId, String category);

    List<SocietyExpenseRecord> findByCommunityIdAndDateBetweenOrderByDateDesc(
            Long communityId, LocalDate from, LocalDate to);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM SocietyExpenseRecord e " +
           "WHERE e.communityId = :cid AND MONTH(e.date) = :month AND YEAR(e.date) = :year")
    BigDecimal sumByCommunityIdAndMonthYear(@Param("cid") Long communityId,
                                            @Param("month") int month,
                                            @Param("year") int year);
}

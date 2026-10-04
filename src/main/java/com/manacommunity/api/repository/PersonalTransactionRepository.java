package com.manacommunity.api.repository;

import com.manacommunity.api.model.PersonalTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface PersonalTransactionRepository extends JpaRepository<PersonalTransaction, Long> {

    List<PersonalTransaction> findByUserIdAndCommunityIdOrderByDateDesc(Long userId, Long communityId);

    List<PersonalTransaction> findByUserIdAndCommunityIdAndTypeOrderByDateDesc(
            Long userId, Long communityId, PersonalTransaction.TransactionType type);

    List<PersonalTransaction> findByUserIdAndCommunityIdAndCategoryOrderByDateDesc(
            Long userId, Long communityId, String category);

    List<PersonalTransaction> findByUserIdAndCommunityIdAndDateBetweenOrderByDateDesc(
            Long userId, Long communityId, LocalDate from, LocalDate to);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM PersonalTransaction t " +
           "WHERE t.user.id = :userId AND t.communityId = :communityId " +
           "AND t.type = :type AND MONTH(t.date) = :month AND YEAR(t.date) = :year")
    BigDecimal sumByUserAndTypeAndMonthYear(@Param("userId") Long userId,
                                            @Param("communityId") Long communityId,
                                            @Param("type") PersonalTransaction.TransactionType type,
                                            @Param("month") int month,
                                            @Param("year") int year);
}

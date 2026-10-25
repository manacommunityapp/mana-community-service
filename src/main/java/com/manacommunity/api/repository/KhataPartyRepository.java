package com.manacommunity.api.repository;

import com.manacommunity.api.model.KhataParty;
import com.manacommunity.api.model.KhataParty.KhataType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface KhataPartyRepository extends JpaRepository<KhataParty, Long> {

    List<KhataParty> findByUserIdAndKhataTypeOrderByLastActivityDesc(Long userId, KhataType khataType);

    Optional<KhataParty> findByIdAndUserId(Long id, Long userId);

    @Query("SELECT COALESCE(SUM(p.balance), 0) FROM KhataParty p WHERE p.user.id = :userId AND p.khataType = :type AND p.balance > 0")
    BigDecimal sumReceivable(@Param("userId") Long userId, @Param("type") KhataType type);

    @Query("SELECT COALESCE(SUM(ABS(p.balance)), 0) FROM KhataParty p WHERE p.user.id = :userId AND p.khataType = :type AND p.balance < 0")
    BigDecimal sumPayable(@Param("userId") Long userId, @Param("type") KhataType type);

    @Query("SELECT COUNT(p) FROM KhataParty p WHERE p.user.id = :userId AND p.khataType = :type AND p.balance <> 0")
    long countWithBalance(@Param("userId") Long userId, @Param("type") KhataType type);
}

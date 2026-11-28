package com.manacommunity.api.clubhouse.repository;

import com.manacommunity.api.clubhouse.entity.FacilityMonthlyQuotaEntity;
import com.manacommunity.api.clubhouse.entity.FacilityMonthlyQuotaId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FacilityMonthlyQuotaRepository
        extends JpaRepository<FacilityMonthlyQuotaEntity, FacilityMonthlyQuotaId> {
}

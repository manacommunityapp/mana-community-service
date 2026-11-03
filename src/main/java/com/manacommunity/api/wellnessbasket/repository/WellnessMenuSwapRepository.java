package com.manacommunity.api.wellnessbasket.repository;

import com.manacommunity.api.wellnessbasket.entity.WellnessMenuSwap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface WellnessMenuSwapRepository extends JpaRepository<WellnessMenuSwap, Long> {

    List<WellnessMenuSwap> findBySubscriberPreferenceIdAndDeliveryDate(Long subscriberPreferenceId, LocalDate deliveryDate);

    List<WellnessMenuSwap> findBySubscriberPreferenceId(Long subscriberPreferenceId);

    List<WellnessMenuSwap> findByStatus(String status);

    List<WellnessMenuSwap> findByDeliveryDateAndStatus(LocalDate deliveryDate, String status);
}

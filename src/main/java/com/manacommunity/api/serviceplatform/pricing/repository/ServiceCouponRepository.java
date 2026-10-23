package com.manacommunity.api.serviceplatform.pricing.repository;
import com.manacommunity.api.serviceplatform.pricing.entity.ServiceCoupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
@Repository
public interface ServiceCouponRepository extends JpaRepository<ServiceCoupon, Long> { Optional<ServiceCoupon> findByCodeAndIsActiveTrue(String code); }

package com.manacommunity.api.devices.repository;

import com.manacommunity.api.devices.entity.DeviceVisitorOtp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface DeviceVisitorOtpRepository extends JpaRepository<DeviceVisitorOtp, Long> {

    @Query("SELECT o FROM DeviceVisitorOtp o WHERE o.deviceId = :deviceId " +
           "AND o.otpCode = :otp AND o.revoked = false AND o.usedAt IS NULL " +
           "AND o.validFrom <= :now AND o.validUntil >= :now")
    Optional<DeviceVisitorOtp> findValid(@Param("deviceId") Long deviceId,
                                         @Param("otp") String otp,
                                         @Param("now") Instant now);
}

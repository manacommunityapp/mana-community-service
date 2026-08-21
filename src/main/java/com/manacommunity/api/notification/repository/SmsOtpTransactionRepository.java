package com.manacommunity.api.notification.repository;

import com.manacommunity.api.notification.entity.SmsOtpTransaction;
import com.manacommunity.api.notification.enums.OtpPurpose;
import com.manacommunity.api.notification.enums.OtpStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SmsOtpTransactionRepository extends JpaRepository<SmsOtpTransaction, Long> {

    /** Latest CREATED OTP for a phone+purpose combination — used for verification. */
    Optional<SmsOtpTransaction> findFirstByPhoneNumberAndPurposeAndStatusOrderByCreatedAtDesc(
            String phoneNumber, OtpPurpose purpose, OtpStatus status);

    /** Count OTPs sent to a phone in the last hour — used for rate limiting. */
    @Query("""
            SELECT COUNT(o) FROM SmsOtpTransaction o
            WHERE o.phoneNumber = :phone
              AND o.purpose = :purpose
              AND o.createdAt >= :since
            """)
    long countRecentSends(@Param("phone") String phone,
                          @Param("purpose") OtpPurpose purpose,
                          @Param("since") LocalDateTime since);

    /** Expire all CREATED OTPs past their expiry time. */
    @Modifying
    @Query("""
            UPDATE SmsOtpTransaction o
            SET o.status = com.manacommunity.api.notification.enums.OtpStatus.EXPIRED
            WHERE o.status = com.manacommunity.api.notification.enums.OtpStatus.CREATED
              AND o.expiresAt < :now
            """)
    int expireStale(@Param("now") LocalDateTime now);

    List<SmsOtpTransaction> findByPhoneNumberAndPurposeOrderByCreatedAtDesc(
            String phoneNumber, OtpPurpose purpose);
}

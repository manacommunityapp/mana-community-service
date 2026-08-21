package com.manacommunity.api.notification.service;

import com.manacommunity.api.exception.SmsOtpExpiredException;
import com.manacommunity.api.exception.SmsOtpVerificationException;
import com.manacommunity.api.notification.config.SmsProperties;
import com.manacommunity.api.notification.dto.SmsOtpRequest;
import com.manacommunity.api.notification.dto.SmsOtpResponse;
import com.manacommunity.api.notification.dto.SmsOtpVerifyRequest;
import com.manacommunity.api.notification.dto.SendSmsRequest;
import com.manacommunity.api.notification.entity.SmsOtpTransaction;
import com.manacommunity.api.notification.enums.MessageType;
import com.manacommunity.api.notification.enums.OtpPurpose;
import com.manacommunity.api.notification.enums.OtpStatus;
import com.manacommunity.api.notification.enums.SmsPriority;
import com.manacommunity.api.notification.repository.SmsOtpTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmsOtpService {

    private final SmsOtpTransactionRepository otpRepo;
    private final SmsService smsService;
    private final PhoneNumberService phoneNumberService;
    private final SmsRateLimiter rateLimiter;
    private final SmsProperties smsProperties;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public SmsOtpResponse sendOtp(SmsOtpRequest req, String clientIp) {
        String normalizedPhone = phoneNumberService.normalize(req.getPhoneNumber());
        SmsProperties.Otp otpConf = smsProperties.getOtp();

        // Per-phone hourly rate limit
        LocalDateTime since = LocalDateTime.now().minusHours(1);
        long recentSends = otpRepo.countRecentSends(normalizedPhone, req.getPurpose(), since);
        if (recentSends >= otpConf.getMaxSendsPerHour()) {
            throw new com.manacommunity.api.exception.SmsRateLimitException(normalizedPhone);
        }

        String otp = generateOtp(otpConf.getLength());
        String hash = passwordEncoder.encode(otp);

        SmsOtpTransaction tx = SmsOtpTransaction.builder()
                .phoneNumber(normalizedPhone)
                .otpHash(hash)
                .purpose(req.getPurpose())
                .status(OtpStatus.CREATED)
                .expiresAt(LocalDateTime.now().plusMinutes(otpConf.getExpiryMinutes()))
                .maxAttempts(otpConf.getMaxAttempts())
                .clientIp(clientIp)
                .build();
        otpRepo.save(tx);

        // Build OTP template key from purpose
        String templateCode = "OTP_" + req.getPurpose().name();
        Long smsId = smsService.send(SendSmsRequest.builder()
                .phoneNumber(normalizedPhone)
                .templateCode(templateCode)
                .variables(Map.of("otp", otp, "expiry", String.valueOf(otpConf.getExpiryMinutes())))
                .messageType(MessageType.OTP)
                .priority(SmsPriority.CRITICAL)
                .build());

        if (smsId != null) {
            tx.setSmsMessageId(smsId);
            otpRepo.save(tx);
        }

        return SmsOtpResponse.builder()
                .success(true)
                .maskedPhone(phoneNumberService.mask(normalizedPhone))
                .expiresAt(tx.getExpiresAt())
                .message("OTP sent successfully")
                .build();
    }

    @Transactional
    public SmsOtpResponse verifyOtp(SmsOtpVerifyRequest req) {
        String normalizedPhone = phoneNumberService.normalize(req.getPhoneNumber());

        SmsOtpTransaction tx = otpRepo
                .findFirstByPhoneNumberAndPurposeAndStatusOrderByCreatedAtDesc(
                        normalizedPhone, req.getPurpose(), OtpStatus.CREATED)
                .orElseThrow(() -> new SmsOtpVerificationException("No active OTP found"));

        if (LocalDateTime.now().isAfter(tx.getExpiresAt())) {
            tx.setStatus(OtpStatus.EXPIRED);
            otpRepo.save(tx);
            throw new SmsOtpExpiredException();
        }

        tx.setAttemptCount(tx.getAttemptCount() + 1);

        if (tx.getAttemptCount() > tx.getMaxAttempts()) {
            tx.setStatus(OtpStatus.BLOCKED);
            otpRepo.save(tx);
            throw new SmsOtpVerificationException("Max verification attempts exceeded");
        }

        if (!passwordEncoder.matches(req.getOtp(), tx.getOtpHash())) {
            otpRepo.save(tx);
            throw new SmsOtpVerificationException("Invalid OTP");
        }

        tx.setStatus(OtpStatus.VERIFIED);
        tx.setVerifiedAt(LocalDateTime.now());
        otpRepo.save(tx);

        return SmsOtpResponse.builder()
                .success(true)
                .maskedPhone(phoneNumberService.mask(normalizedPhone))
                .message("OTP verified successfully")
                .build();
    }

    private String generateOtp(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(secureRandom.nextInt(10));
        }
        return sb.toString();
    }
}

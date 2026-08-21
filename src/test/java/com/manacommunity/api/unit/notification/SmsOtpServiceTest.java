package com.manacommunity.api.unit.notification;

import com.manacommunity.api.exception.SmsOtpExpiredException;
import com.manacommunity.api.exception.SmsOtpVerificationException;
import com.manacommunity.api.exception.SmsRateLimitException;
import com.manacommunity.api.notification.config.SmsProperties;
import com.manacommunity.api.notification.dto.SmsOtpRequest;
import com.manacommunity.api.notification.dto.SmsOtpResponse;
import com.manacommunity.api.notification.dto.SmsOtpVerifyRequest;
import com.manacommunity.api.notification.entity.SmsOtpTransaction;
import com.manacommunity.api.notification.enums.OtpPurpose;
import com.manacommunity.api.notification.enums.OtpStatus;
import com.manacommunity.api.notification.repository.SmsOtpTransactionRepository;
import com.manacommunity.api.notification.service.PhoneNumberService;
import com.manacommunity.api.notification.service.SmsOtpService;
import com.manacommunity.api.notification.service.SmsRateLimiter;
import com.manacommunity.api.notification.service.SmsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SmsOtpServiceTest {

    @Mock private SmsOtpTransactionRepository otpRepo;
    @Mock private SmsService smsService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4); // low cost for tests
    private SmsOtpService smsOtpService;
    private SmsProperties props;

    @BeforeEach
    void setUp() {
        props = new SmsProperties();
        props.setDefaultCountryCode("+91");
        props.setEnabled(true);

        PhoneNumberService phoneService = new PhoneNumberService(props);
        SmsRateLimiter rateLimiter = new SmsRateLimiter(props);

        smsOtpService = new SmsOtpService(otpRepo, smsService, phoneService, rateLimiter, props, passwordEncoder);
    }

    // ── sendOtp() ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("sendOtp()")
    class SendOtpTests {

        @Test
        @DisplayName("persists OTP hash, sends SMS, returns masked phone")
        void successFlow() {
            when(otpRepo.countRecentSends(any(), any(), any())).thenReturn(0L);
            when(otpRepo.save(any())).thenAnswer(inv -> {
                SmsOtpTransaction tx = inv.getArgument(0);
                tx = SmsOtpTransaction.builder()
                        .id(1L).phoneNumber(tx.getPhoneNumber())
                        .otpHash(tx.getOtpHash()).purpose(tx.getPurpose())
                        .status(tx.getStatus()).expiresAt(tx.getExpiresAt())
                        .maxAttempts(tx.getMaxAttempts()).clientIp(tx.getClientIp())
                        .build();
                return tx;
            });
            when(smsService.send(any())).thenReturn(10L);

            SmsOtpRequest req = new SmsOtpRequest();
            req.setPhoneNumber("9876543210");
            req.setPurpose(OtpPurpose.LOGIN);

            SmsOtpResponse resp = smsOtpService.sendOtp(req, "10.0.0.1");

            assertThat(resp.isSuccess()).isTrue();
            assertThat(resp.getMaskedPhone()).contains("******").doesNotContain("987654");
            assertThat(resp.getExpiresAt()).isAfter(LocalDateTime.now());
            verify(otpRepo, atLeastOnce()).save(any());
            verify(smsService).send(any());
        }

        @Test
        @DisplayName("stores bcrypt hash, never raw OTP")
        void storesHashNotRawOtp() {
            when(otpRepo.countRecentSends(any(), any(), any())).thenReturn(0L);
            when(smsService.send(any())).thenReturn(5L);
            ArgumentCaptor<SmsOtpTransaction> captor = ArgumentCaptor.forClass(SmsOtpTransaction.class);

            SmsOtpTransaction returned = SmsOtpTransaction.builder()
                    .id(1L).phoneNumber("+919876543210").otpHash("$2a$...")
                    .purpose(OtpPurpose.REGISTRATION).status(OtpStatus.CREATED)
                    .expiresAt(LocalDateTime.now().plusMinutes(5)).maxAttempts(5).build();
            when(otpRepo.save(any())).thenReturn(returned);

            SmsOtpRequest req = new SmsOtpRequest();
            req.setPhoneNumber("9876543210");
            req.setPurpose(OtpPurpose.REGISTRATION);
            smsOtpService.sendOtp(req, "127.0.0.1");

            verify(otpRepo, atLeastOnce()).save(captor.capture());
            String savedHash = captor.getAllValues().get(0).getOtpHash();
            // Hash starts with BCrypt prefix, never a plain 6-digit number
            assertThat(savedHash).startsWith("$2a$");
            assertThat(savedHash).doesNotMatch("\\d{6}");
        }

        @Test
        @DisplayName("uses OTP_<PURPOSE> as template code")
        void templateCodeMappedFromPurpose() {
            when(otpRepo.countRecentSends(any(), any(), any())).thenReturn(0L);
            SmsOtpTransaction returned = SmsOtpTransaction.builder()
                    .id(1L).phoneNumber("+919876543210").otpHash("$2a$")
                    .purpose(OtpPurpose.PASSWORD_RESET).status(OtpStatus.CREATED)
                    .expiresAt(LocalDateTime.now().plusMinutes(5)).maxAttempts(5).build();
            when(otpRepo.save(any())).thenReturn(returned);
            when(smsService.send(any())).thenReturn(1L);

            SmsOtpRequest req = new SmsOtpRequest();
            req.setPhoneNumber("9876543210");
            req.setPurpose(OtpPurpose.PASSWORD_RESET);
            smsOtpService.sendOtp(req, "127.0.0.1");

            ArgumentCaptor<com.manacommunity.api.notification.dto.SendSmsRequest> smsCap =
                    ArgumentCaptor.forClass(com.manacommunity.api.notification.dto.SendSmsRequest.class);
            verify(smsService).send(smsCap.capture());
            assertThat(smsCap.getValue().getTemplateCode()).isEqualTo("OTP_PASSWORD_RESET");
        }

        @Test
        @DisplayName("throws SmsRateLimitException when hourly sends exhausted")
        void rateLimitExceeded() {
            long maxSends = props.getOtp().getMaxSendsPerHour();
            when(otpRepo.countRecentSends(any(), any(), any())).thenReturn(maxSends);

            SmsOtpRequest req = new SmsOtpRequest();
            req.setPhoneNumber("9876543210");
            req.setPurpose(OtpPurpose.LOGIN);

            assertThatThrownBy(() -> smsOtpService.sendOtp(req, "127.0.0.1"))
                    .isInstanceOf(SmsRateLimitException.class);

            verify(otpRepo, never()).save(any());
        }

        @Test
        @DisplayName("sets clientIp on transaction")
        void clientIpRecorded() {
            when(otpRepo.countRecentSends(any(), any(), any())).thenReturn(0L);
            when(smsService.send(any())).thenReturn(1L);

            SmsOtpTransaction returned = SmsOtpTransaction.builder()
                    .id(1L).phoneNumber("+919876543210").otpHash("$2a$")
                    .purpose(OtpPurpose.LOGIN).status(OtpStatus.CREATED)
                    .expiresAt(LocalDateTime.now().plusMinutes(5)).maxAttempts(5)
                    .clientIp("203.0.113.5").build();
            when(otpRepo.save(any())).thenReturn(returned);

            SmsOtpRequest req = new SmsOtpRequest();
            req.setPhoneNumber("9876543210");
            req.setPurpose(OtpPurpose.LOGIN);
            smsOtpService.sendOtp(req, "203.0.113.5");

            ArgumentCaptor<SmsOtpTransaction> captor = ArgumentCaptor.forClass(SmsOtpTransaction.class);
            verify(otpRepo, atLeastOnce()).save(captor.capture());
            assertThat(captor.getAllValues().get(0).getClientIp()).isEqualTo("203.0.113.5");
        }
    }

    // ── verifyOtp() ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("verifyOtp()")
    class VerifyOtpTests {

        private SmsOtpTransaction activeTx(String rawOtp) {
            return SmsOtpTransaction.builder()
                    .id(1L).phoneNumber("+919876543210")
                    .otpHash(passwordEncoder.encode(rawOtp))
                    .purpose(OtpPurpose.LOGIN).status(OtpStatus.CREATED)
                    .expiresAt(LocalDateTime.now().plusMinutes(5))
                    .attemptCount(0).maxAttempts(5).build();
        }

        @Test
        @DisplayName("correct OTP → VERIFIED, returns success response")
        void correctOtpVerified() {
            SmsOtpTransaction tx = activeTx("123456");
            when(otpRepo.findFirstByPhoneNumberAndPurposeAndStatusOrderByCreatedAtDesc(
                    "+919876543210", OtpPurpose.LOGIN, OtpStatus.CREATED))
                    .thenReturn(Optional.of(tx));
            when(otpRepo.save(any())).thenReturn(tx);

            SmsOtpVerifyRequest req = new SmsOtpVerifyRequest();
            req.setPhoneNumber("9876543210");
            req.setOtp("123456");
            req.setPurpose(OtpPurpose.LOGIN);

            SmsOtpResponse resp = smsOtpService.verifyOtp(req);

            assertThat(resp.isSuccess()).isTrue();
            assertThat(tx.getStatus()).isEqualTo(OtpStatus.VERIFIED);
            assertThat(tx.getVerifiedAt()).isNotNull();
        }

        @Test
        @DisplayName("wrong OTP increments attemptCount and throws SmsOtpVerificationException")
        void wrongOtpIncrementsCount() {
            SmsOtpTransaction tx = activeTx("111111");
            when(otpRepo.findFirstByPhoneNumberAndPurposeAndStatusOrderByCreatedAtDesc(
                    "+919876543210", OtpPurpose.LOGIN, OtpStatus.CREATED))
                    .thenReturn(Optional.of(tx));
            when(otpRepo.save(any())).thenReturn(tx);

            SmsOtpVerifyRequest req = new SmsOtpVerifyRequest();
            req.setPhoneNumber("9876543210");
            req.setOtp("999999");
            req.setPurpose(OtpPurpose.LOGIN);

            assertThatThrownBy(() -> smsOtpService.verifyOtp(req))
                    .isInstanceOf(SmsOtpVerificationException.class)
                    .hasMessageContaining("Invalid OTP");

            assertThat(tx.getAttemptCount()).isEqualTo(1);
            verify(otpRepo, atLeastOnce()).save(tx);
        }

        @Test
        @DisplayName("expired OTP → status=EXPIRED and throws SmsOtpExpiredException")
        void expiredOtpThrows() {
            SmsOtpTransaction tx = SmsOtpTransaction.builder()
                    .id(2L).phoneNumber("+919876543210")
                    .otpHash(passwordEncoder.encode("123456"))
                    .purpose(OtpPurpose.LOGIN).status(OtpStatus.CREATED)
                    .expiresAt(LocalDateTime.now().minusMinutes(1))
                    .attemptCount(0).maxAttempts(5).build();
            when(otpRepo.findFirstByPhoneNumberAndPurposeAndStatusOrderByCreatedAtDesc(
                    "+919876543210", OtpPurpose.LOGIN, OtpStatus.CREATED))
                    .thenReturn(Optional.of(tx));
            when(otpRepo.save(any())).thenReturn(tx);

            SmsOtpVerifyRequest req = new SmsOtpVerifyRequest();
            req.setPhoneNumber("9876543210");
            req.setOtp("123456");
            req.setPurpose(OtpPurpose.LOGIN);

            assertThatThrownBy(() -> smsOtpService.verifyOtp(req))
                    .isInstanceOf(SmsOtpExpiredException.class);

            assertThat(tx.getStatus()).isEqualTo(OtpStatus.EXPIRED);
        }

        @Test
        @DisplayName("exceeding maxAttempts → status=BLOCKED, throws")
        void maxAttemptsBlocksUser() {
            // At maxAttempts=5, the 6th attempt (count becomes 6) triggers block
            SmsOtpTransaction tx = SmsOtpTransaction.builder()
                    .id(3L).phoneNumber("+919876543210")
                    .otpHash(passwordEncoder.encode("123456"))
                    .purpose(OtpPurpose.LOGIN).status(OtpStatus.CREATED)
                    .expiresAt(LocalDateTime.now().plusMinutes(5))
                    .attemptCount(5).maxAttempts(5).build();
            when(otpRepo.findFirstByPhoneNumberAndPurposeAndStatusOrderByCreatedAtDesc(
                    "+919876543210", OtpPurpose.LOGIN, OtpStatus.CREATED))
                    .thenReturn(Optional.of(tx));
            when(otpRepo.save(any())).thenReturn(tx);

            SmsOtpVerifyRequest req = new SmsOtpVerifyRequest();
            req.setPhoneNumber("9876543210");
            req.setOtp("123456");
            req.setPurpose(OtpPurpose.LOGIN);

            assertThatThrownBy(() -> smsOtpService.verifyOtp(req))
                    .isInstanceOf(SmsOtpVerificationException.class)
                    .hasMessageContaining("Max verification attempts exceeded");

            assertThat(tx.getStatus()).isEqualTo(OtpStatus.BLOCKED);
        }

        @Test
        @DisplayName("no active OTP found → throws SmsOtpVerificationException")
        void noActiveOtpThrows() {
            when(otpRepo.findFirstByPhoneNumberAndPurposeAndStatusOrderByCreatedAtDesc(
                    "+919876543210", OtpPurpose.LOGIN, OtpStatus.CREATED))
                    .thenReturn(Optional.empty());

            SmsOtpVerifyRequest req = new SmsOtpVerifyRequest();
            req.setPhoneNumber("9876543210");
            req.setOtp("123456");
            req.setPurpose(OtpPurpose.LOGIN);

            assertThatThrownBy(() -> smsOtpService.verifyOtp(req))
                    .isInstanceOf(SmsOtpVerificationException.class)
                    .hasMessageContaining("No active OTP found");
        }
    }
}

package com.manacommunity.api.unit.service;

import com.manacommunity.api.config.RegistrationSecurityProperties;
import com.manacommunity.api.dto.otp.OtpResponse;
import com.manacommunity.api.email.EmailService;
import com.manacommunity.api.email.EmailSupport;
import com.manacommunity.api.email.EmailTemplateRenderer;
import com.manacommunity.api.model.EmailOtp;
import com.manacommunity.api.repository.EmailOtpRepository;
import com.manacommunity.api.service.OtpService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OtpService Tests")
class OtpServiceTest {

    @Mock
    private RegistrationSecurityProperties props;

    @Mock
    private EmailOtpRepository otpRepo;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @Mock
    private EmailTemplateRenderer renderer;

    @Mock
    private EmailSupport support;

    @InjectMocks
    private OtpService otpService;

    private RegistrationSecurityProperties.Otp otpConfig;

    @BeforeEach
    void setUp() {
        otpConfig = new RegistrationSecurityProperties.Otp();
        // Verify default TTL is 5 minutes
        assertThat(otpConfig.getTtlMinutes()).isEqualTo(5);
        when(props.getOtp()).thenReturn(otpConfig);
    }

    @Test
    @DisplayName("send() should save OTP with 5-minute expiry")
    void sendOtp_shouldSetFiveMinutesExpiry() {
        when(otpRepo.countByEmailAndCreatedAtAfter(anyString(), any(LocalDateTime.class))).thenReturn(0L);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed-otp");
        when(support.baseVars(anyString())).thenReturn(new HashMap<>());

        OtpResponse response = otpService.send("user@example.com", "User");

        assertThat(response).isNotNull();
        assertThat(response.success()).isTrue();

        ArgumentCaptor<EmailOtp> captor = ArgumentCaptor.forClass(EmailOtp.class);
        verify(otpRepo).save(captor.capture());
        EmailOtp savedOtp = captor.getValue();

        assertThat(savedOtp.getEmail()).isEqualTo("user@example.com");
        assertThat(savedOtp.getExpiresAt()).isAfter(LocalDateTime.now().plusMinutes(4));
        assertThat(savedOtp.getExpiresAt()).isBeforeOrEqualTo(LocalDateTime.now().plusMinutes(5).plusSeconds(5));
    }
}

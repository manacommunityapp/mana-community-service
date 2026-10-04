package com.manacommunity.api.access.biometric;

import com.manacommunity.api.access.biometric.dto.BiometricDtos.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface BiometricTurnstileService {
    VerifyFaceResult verifyFace(VerifyFaceRequest request);
    void enrollUser(EnrollBiometricRequest request);
    List<BiometricTurnstileDto> getTurnstilesForCommunity(Long communityId);
    Page<BiometricAccessLogDto> getAccessLogs(Long communityId, Pageable pageable);
    void setTurnstileStatus(Long turnstileId, BiometricEnums.TurnstileStatus status);
}

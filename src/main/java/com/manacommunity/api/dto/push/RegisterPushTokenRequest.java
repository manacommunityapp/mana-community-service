package com.manacommunity.api.dto.push;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class RegisterPushTokenRequest {

    /**
     * Expo push token: "ExponentPushToken[...]"
     * The mobile hook sends this automatically after permission is granted.
     */
    @NotBlank(message = "token is required")
    private String token;

    /** "ios" or "android" */
    @NotBlank
    @Pattern(regexp = "ios|android", message = "platform must be 'ios' or 'android'")
    private String platform;
}

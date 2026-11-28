package com.manacommunity.api.versioning;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;

/**
 * Rejects calls to @RequireAppVersion-guarded endpoints when the client's
 * X-App-Version header is below the required minimum.
 *
 * Response on failure: HTTP 426 Upgrade Required
 *   {"error": "UPGRADE_REQUIRED", "minimumVersion": "2.5.0",
 *    "message": "Please update the app to version 2.5.0 or later."}
 *
 * Register this interceptor in WebMvcConfig.addInterceptors().
 */
@Component
@RequiredArgsConstructor
public class AppVersionGatingInterceptor implements HandlerInterceptor {

    private static final String VERSION_HEADER = "X-App-Version";
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        if (!(handler instanceof HandlerMethod hm)) return true;

        RequireAppVersion ann = AnnotationUtils.findAnnotation(hm.getMethod(), RequireAppVersion.class);
        if (ann == null) return true;

        String clientVersion = request.getHeader(VERSION_HEADER);
        if (clientVersion == null || !meetsMinimum(clientVersion, ann.value())) {
            response.setStatus(HttpStatus.UPGRADE_REQUIRED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(objectMapper.writeValueAsString(Map.of(
                "error", "UPGRADE_REQUIRED",
                "minimumVersion", ann.value(),
                "message", "Please update the app to version " + ann.value() + " or later."
            )));
            return false;
        }
        return true;
    }

    /**
     * Semver-style comparison. "1.10.0" > "1.9.0".
     * Returns true when clientVersion >= minimum.
     */
    static boolean meetsMinimum(String clientVersion, String minimum) {
        int[] c = parse(clientVersion);
        int[] m = parse(minimum);
        for (int i = 0; i < Math.max(c.length, m.length); i++) {
            int cv = i < c.length ? c[i] : 0;
            int mv = i < m.length ? m[i] : 0;
            if (cv != mv) return cv > mv;
        }
        return true;
    }

    private static int[] parse(String version) {
        String[] parts = version.replaceAll("[^0-9.]", "").split("\\.");
        int[] nums = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try { nums[i] = Integer.parseInt(parts[i]); } catch (NumberFormatException ignored) {}
        }
        return nums;
    }
}

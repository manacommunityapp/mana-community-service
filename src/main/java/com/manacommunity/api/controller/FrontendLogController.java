package com.manacommunity.api.controller;

import com.manacommunity.api.security.UserPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/frontend-logs")
@Validated
public class FrontendLogController {

    private static final Logger log = LoggerFactory.getLogger("FRONTEND");

    @PostMapping
    public ResponseEntity<Void> ingest(
            @RequestBody @Valid @Size(max = 100) List<FrontendLogEntry> entries,
            @AuthenticationPrincipal UserPrincipal principal) {

        String userId = principal != null ? String.valueOf(principal.getId()) : "-";

        for (FrontendLogEntry entry : entries) {
            MDC.put("correlationId", sanitize(entry.getCorrelationId()));
            MDC.put("userId", userId);
            try {
                String msg = String.format("[%s] [%s] %s%s",
                        sanitize(entry.getTag()),
                        sanitize(entry.getTs()),
                        sanitize(entry.getMessage()),
                        entry.getError() != null ? " | error=" + sanitize(entry.getError()) : "");

                switch (entry.getLevel() != null ? entry.getLevel().toUpperCase() : "ERROR") {
                    case "WARN" -> log.warn(msg);
                    default -> log.error(msg);
                }
            } finally {
                MDC.remove("correlationId");
                MDC.remove("userId");
            }
        }
        return ResponseEntity.noContent().build();
    }

    private static String sanitize(String input) {
        if (input == null) return "-";
        return input.replaceAll("[\\r\\n]", " ").substring(0, Math.min(input.length(), 2000));
    }

    @Data
    public static class FrontendLogEntry {
        @NotBlank @Size(max = 10) private String level;
        @NotBlank @Size(max = 50) private String ts;
        @NotBlank @Size(max = 100) private String tag;
        @NotBlank @Size(max = 2000) private String message;
        @Size(max = 100) private String correlationId;
        @Size(max = 5000) private String error;
        @Size(max = 10000) private String stack;
        @Size(max = 2000) private String data;
    }
}

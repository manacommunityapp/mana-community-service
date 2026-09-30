package com.manacommunity.api.audit;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditContext {
    private String tenantId;
    private Long userId;
    private String username;
    private String userRole;
    private String correlationId;
    private String ipAddress;
    private String deviceInfo;
    private String userAgent;
    private String serviceName;
    private String endpoint;
    private String location;
}

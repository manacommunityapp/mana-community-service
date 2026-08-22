package com.manacommunity.api.email.brevo;

import lombok.Builder;
import java.util.List;

@Builder
public record BrevoSenderDto(
        long id,
        String name,
        String email,
        boolean active,
        List<String> ips
) {}
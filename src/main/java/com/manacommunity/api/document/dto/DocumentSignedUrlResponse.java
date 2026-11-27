package com.manacommunity.api.document.dto;

import java.time.Instant;

public record DocumentSignedUrlResponse(String url, Instant expiresAt) {}

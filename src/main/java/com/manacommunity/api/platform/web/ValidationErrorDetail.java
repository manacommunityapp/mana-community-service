package com.manacommunity.api.platform.web;

/**
 * Per-field validation error detail, embedded in {@link ApiResponse} when
 * request validation fails.
 */
public record ValidationErrorDetail(String field, String message, Object rejectedValue) {}

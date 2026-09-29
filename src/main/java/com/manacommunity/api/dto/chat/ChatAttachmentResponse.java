package com.manacommunity.api.dto.chat;

public record ChatAttachmentResponse(
        Long id,
        String fileUrl,
        String fileName,
        String contentType,
        Long sizeBytes
) {}

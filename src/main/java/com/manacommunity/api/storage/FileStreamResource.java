package com.manacommunity.api.storage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileStreamResource {
    private StreamingResponseBody body;
    private String contentType;
    private Long contentLength;
    private String eTag;
    private String filename;
}

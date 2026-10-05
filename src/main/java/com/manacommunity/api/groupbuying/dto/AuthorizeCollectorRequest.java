package com.manacommunity.api.groupbuying.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthorizeCollectorRequest {

    @NotBlank
    private String name;

    private String relationship;
    private String phone;
}

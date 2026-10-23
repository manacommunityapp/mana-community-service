package com.manacommunity.api.dto.admin;

import lombok.Data;

/**
 * Optional body for member action endpoints.
 * reason is used by reject and suspend to record why the action was taken.
 */
@Data
public class MemberActionRequest {
    private String reason;
}

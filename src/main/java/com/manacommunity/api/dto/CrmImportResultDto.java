package com.manacommunity.api.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class CrmImportResultDto {
    private int created;
    private int duplicates;
    private int errors;
    private List<String> errorMessages;
}

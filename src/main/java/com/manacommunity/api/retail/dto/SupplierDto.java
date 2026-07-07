package com.manacommunity.api.retail.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierDto {

    private Long id;
    private String name;
    private String contactPerson;
    private String phone;
    private String email;
}

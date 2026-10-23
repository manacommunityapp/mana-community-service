package com.manacommunity.api.wellness.dto;

import com.manacommunity.api.wellness.enums.WellnessEnums.*;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WellnessProviderDto {
    private Long id;
    private String name;
    private WellnessProviderType providerType;
    private String specialty;
    private String description;
    private Integer experienceYears;
    private BigDecimal rating;
    private Integer totalReviews;
    private BigDecimal consultationFee;
    private Boolean availableOnline;
    private Boolean availableInPerson;
    private String location;
    private String phone;
    private String email;
    private String photoUrl;
    private Long communityId;
    private Boolean verified;
    private Boolean active;
}

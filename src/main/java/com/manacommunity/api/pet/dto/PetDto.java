package com.manacommunity.api.pet.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PetDto {
    private Long id;
    private String name;
    private String species;
    private String breed;
    private String color;
    private Integer ageMonths;
    private BigDecimal weightKg;
    private String microchipId;
    private boolean isVaccinated;
    private LocalDate vaccineExpiryDate;
    private LocalDate registrationDate;
    private String status;
    private String imageUrl;
    private String ownerName;
    private String ownerFlat;
    private Long communityId;
}

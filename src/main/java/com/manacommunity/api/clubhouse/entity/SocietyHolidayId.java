package com.manacommunity.api.clubhouse.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SocietyHolidayId implements Serializable {

    @Column(name = "society_id", nullable = false)
    private Long societyId;

    @Column(name = "holiday_date", nullable = false)
    private LocalDate holidayDate;
}

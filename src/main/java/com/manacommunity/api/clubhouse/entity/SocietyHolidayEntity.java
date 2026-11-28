package com.manacommunity.api.clubhouse.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "society_holidays", schema = "clubhouse")
@Getter
@Setter
@NoArgsConstructor
public class SocietyHolidayEntity {

    @EmbeddedId
    private SocietyHolidayId id;

    @Column(name = "holiday_name", nullable = false, length = 120)
    private String holidayName;
}

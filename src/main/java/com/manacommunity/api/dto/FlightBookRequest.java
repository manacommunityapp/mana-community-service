package com.manacommunity.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

@Data
public class FlightBookRequest {

    @NotBlank
    private String searchId;

    @NotBlank
    private String optionId;

    @NotNull
    @Valid
    @Size(min = 1)
    private List<PassengerInfo> passengers;

    private String contactEmail;

    @Pattern(regexp = "^\\+?[0-9]{10,13}$", message = "Invalid contact phone number")
    private String contactPhone;

    @Data
    public static class PassengerInfo {
        @NotBlank
        private String firstName;

        @NotBlank
        private String lastName;

        @NotBlank
        private String dateOfBirth;   // "YYYY-MM-DD"

        @NotBlank
        private String gender;        // MALE | FEMALE | OTHER

        @NotBlank
        @Pattern(regexp = "^(ADULT|CHILD|INFANT)$")
        private String passengerType;

        private String passportNumber;
        private String passportExpiry;
        private String nationality;

        // SSR selections (optional)
        private String seatPreference;   // WINDOW | AISLE | MIDDLE
        private String mealPreference;   // VEG | NON_VEG | JAIN | VEGAN
        private int extraBaggageKg;      // 0 = no extra
    }
}

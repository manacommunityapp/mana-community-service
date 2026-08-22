package com.manacommunity.api.events.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EventRequest {

    @NotBlank(message = "Event title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    @Size(max = 3000)
    private String description;

    private String type;

    private String startDate;
    private String endDate;
    private String startTime;
    private String endTime;

    private String locationType;

    @Size(max = 300)
    private String location;

    private String priceType;

    @DecimalMin(value = "0.0", message = "Price must be zero or positive")
    private Double price;

    @Min(value = 1, message = "Capacity must be at least 1")
    private Integer capacity;

    @Size(max = 500)
    private String imageUrl;
    private String imageMediaId;

    @Size(max = 200)
    private String organizerName;

    @Size(max = 200)
    private String organizerContact;

    private Long venueId;

    @Size(max = 200)
    private String venue;

    @Size(max = 100)
    private String city;

    @Size(max = 100)
    private String category;

    private String status;

    @Size(max = 255)
    private String paymentModes;

    @Size(max = 100)
    private String upiId;

    @Size(max = 500)
    private String scannerUrl;
    private String scannerMediaId;

    @Size(max = 3000)
    private String notes;

    @Size(max = 3000)
    private String contactsJson;

    @Size(max = 2000)
    private String paymentInstructions;

    private java.util.List<TicketTypeDto> ticketTypes;

    private String ticketTypesJson;

    @Min(value = 1, message = "Max attendees must be at least 1")
    private Integer maxAttendees;

    private String registrationDeadline;
}

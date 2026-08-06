package com.manacommunity.api.events.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EventTaskRequest {

    @NotNull
    private Long eventId;

    @NotBlank
    private String title;


    private String description;
    private String phase;
    private String priority;
    private String assigneeName;
    private Long assigneeId;
    private String dueDate;

    private LocalDate dueDate;

}

package com.manacommunity.api.sports.dto;

import com.manacommunity.api.dto.RegistrationRequest;
import lombok.Data;
import java.util.List;

@Data
public class SportsMultiRegisterRequest {
    private List<RegistrationRequest> registrations;
}

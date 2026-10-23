package com.manacommunity.api.parking.service;

import com.manacommunity.api.parking.dto.ParkingSpotDto;
import com.manacommunity.api.parking.dto.ReserveSpotRequest;
import com.manacommunity.api.parking.dto.VisitorPassDto;
import com.manacommunity.api.parking.dto.VisitorPassRequest;
import com.manacommunity.api.user.model.AppUser;

import java.util.List;

public interface ParkingService {

    List<ParkingSpotDto> getSpots(AppUser user, String type, String status, String level);

    List<ParkingSpotDto> getMySpots(AppUser user);

    ParkingSpotDto reserveSpot(AppUser user, ReserveSpotRequest request);

    VisitorPassDto createVisitorPass(AppUser user, VisitorPassRequest request);
}

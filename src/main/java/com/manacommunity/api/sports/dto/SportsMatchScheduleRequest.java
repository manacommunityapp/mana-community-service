package com.manacommunity.api.sports.dto;
import com.manacommunity.api.sports.repository.*;
import com.manacommunity.api.sports.model.*;
import com.manacommunity.api.sports.dto.*;

public record SportsMatchScheduleRequest(Long homeTeamId, Long awayTeamId, String matchType, String stage, String startTime) {}

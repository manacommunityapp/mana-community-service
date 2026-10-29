package com.manacommunity.api.service;

import com.manacommunity.api.dto.*;

public interface FlightGatewayService {

    FlightSearchResponse search(FlightSearchRequest request);

    FlightFareQuoteResponse fareQuote(FlightFareQuoteRequest request);

    FlightBookResponse book(FlightBookRequest request);

    FlightTicketResponse issueTicket(FlightTicketRequest request);
}

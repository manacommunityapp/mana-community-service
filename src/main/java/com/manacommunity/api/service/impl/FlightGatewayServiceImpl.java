package com.manacommunity.api.service.impl;

import com.manacommunity.api.dto.*;
import com.manacommunity.api.exception.ServiceDegradedException;
import com.manacommunity.api.service.FlightGatewayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@Slf4j
public class FlightGatewayServiceImpl implements FlightGatewayService {

    private final RestTemplate restTemplate;

    public FlightGatewayServiceImpl(@Qualifier("flightRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Value("${app.tripjack.base-url}")
    private String baseUrl;

    @Value("${app.tripjack.api-key}")
    private String apiKey;

    // ── Search ─────────────────────────────────────────────────────────────

    @Override
    public FlightSearchResponse search(FlightSearchRequest req) {
        Map<String, Object> payload = buildSearchPayload(req);
        Map<?, ?> raw = post("/fms/v1/air-search-all", payload);
        return mapSearchResponse(raw);
    }

    // ── Fare Re-validate ───────────────────────────────────────────────────

    @Override
    public FlightFareQuoteResponse fareQuote(FlightFareQuoteRequest req) {
        Map<String, Object> payload = Map.of(
                "searchQuery", Map.of(
                        "resultIndex", req.getOptionId(),
                        "srp_Id", req.getSearchId()
                )
        );
        Map<?, ?> raw = post("/fms/v1/review", payload);
        return mapFareQuoteResponse(raw, req.getOptionId());
    }

    // ── Book (Hold PNR) ────────────────────────────────────────────────────

    @Override
    public FlightBookResponse book(FlightBookRequest req) {
        Map<String, Object> payload = buildBookPayload(req);
        Map<?, ?> raw = post("/fms/v1/book", payload);
        return mapBookResponse(raw);
    }

    // ── Issue Ticket (post Razorpay payment) ──────────────────────────────

    @Override
    public FlightTicketResponse issueTicket(FlightTicketRequest req) {
        Map<String, Object> payload = Map.of(
                "bookingId", req.getBookingId()
        );
        Map<?, ?> raw = post("/fms/v1/ticket", payload);
        return mapTicketResponse(raw);
    }

    // ── HTTP helper ────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private Map<?, ?> post(String path, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("apikey", apiKey);
        HttpEntity<Object> entity = new HttpEntity<>(body, headers);
        try {
            ResponseEntity<Map> resp = restTemplate.exchange(
                    baseUrl + path, HttpMethod.POST, entity, Map.class);
            return Optional.ofNullable(resp.getBody()).orElse(Collections.emptyMap());
        } catch (HttpClientErrorException e) {
            log.error("Tripjack API error {}: {}", path, e.getResponseBodyAsString());
            throw new ServiceDegradedException("Flight gateway unavailable. Please try again.");
        } catch (Exception e) {
            log.error("Tripjack call failed for {}: {}", path, e.getMessage());
            throw new ServiceDegradedException("Flight gateway unavailable. Please try again.");
        }
    }

    // ── Payload builders ───────────────────────────────────────────────────

    private Map<String, Object> buildSearchPayload(FlightSearchRequest req) {
        Map<String, Object> searchQuery = new LinkedHashMap<>();
        searchQuery.put("cabinClass", req.getCabinClass());
        searchQuery.put("paxInfo", Map.of(
                "ADULT", req.getAdults(),
                "CHILD", req.getChildren(),
                "INFANT", req.getInfants()
        ));
        searchQuery.put("routeInfos", List.of(Map.of(
                "fromCityOrAirport", Map.of("code", req.getOrigin()),
                "toCityOrAirport", Map.of("code", req.getDestination()),
                "travelDate", req.getDepartureDate()
        )));
        searchQuery.put("searchModifiers", Map.of("isDirectFlight", req.isDirectOnly()));
        return Map.of("searchQuery", searchQuery);
    }

    private Map<String, Object> buildBookPayload(FlightBookRequest req) {
        List<Map<String, Object>> paxList = new ArrayList<>();
        for (FlightBookRequest.PassengerInfo p : req.getPassengers()) {
            Map<String, Object> pax = new LinkedHashMap<>();
            pax.put("fN", p.getFirstName());
            pax.put("lN", p.getLastName());
            pax.put("dob", p.getDateOfBirth());
            pax.put("gender", p.getGender());
            pax.put("paxType", p.getPassengerType());
            if (p.getPassportNumber() != null) {
                pax.put("pid", p.getPassportNumber());
                pax.put("ppExpiry", p.getPassportExpiry());
                pax.put("nationality", p.getNationality());
            }
            paxList.add(pax);
        }
        return Map.of(
                "bookQuery", Map.of(
                        "resultIndex", req.getOptionId(),
                        "srp_Id", req.getSearchId(),
                        "travellerInfo", paxList
                )
        );
    }

    // ── Response mappers (adapt Tripjack JSON shape to our DTOs) ──────────

    @SuppressWarnings("unchecked")
    private FlightSearchResponse mapSearchResponse(Map<?, ?> raw) {
        List<FlightSearchResponse.FlightOption> options = new ArrayList<>();
        try {
            Map<?, ?> results = (Map<?, ?>) raw.get("searchResult");
            if (results == null) return FlightSearchResponse.builder().flights(options).build();
            List<?> tripResults = (List<?>) results.get("tripInfos");
            if (tripResults == null) return FlightSearchResponse.builder().flights(options).build();

            String searchId = (String) results.get("srp_Id");
            for (Object r : tripResults) {
                Map<?, ?> result = (Map<?, ?>) r;
                List<?> sInfos = (List<?>) result.get("sI");
                if (sInfos == null) continue;

                List<FlightSearchResponse.FlightSegment> segments = new ArrayList<>();
                for (Object si : sInfos) {
                    Map<?, ?> s = (Map<?, ?>) si;
                    Map<?, ?> fD = (Map<?, ?>) s.get("fD");
                    Map<?, ?> da = (Map<?, ?>) s.get("da");
                    Map<?, ?> aa = (Map<?, ?>) s.get("aa");
                    segments.add(FlightSearchResponse.FlightSegment.builder()
                            .flightNumber(fD != null ? fD.get("fN") + "" : "")
                            .airlineCode(fD != null ? (String) ((Map<?, ?>) fD.get("aI")).get("code") : "")
                            .airlineName(fD != null ? (String) ((Map<?, ?>) fD.get("aI")).get("name") : "")
                            .originCode(da != null ? (String) da.get("code") : "")
                            .originCity(da != null ? (String) da.get("cityCode") : "")
                            .originTime((String) s.get("dt"))
                            .destinationCode(aa != null ? (String) aa.get("code") : "")
                            .destinationCity(aa != null ? (String) aa.get("cityCode") : "")
                            .destinationTime((String) s.get("at"))
                            .durationMinutes(s.get("duration") instanceof Number n ? n.intValue() : 0)
                            .build());
                }

                Map<?, ?> totalPriceList = (Map<?, ?>) result.get("totalPriceList");
                double totalFare = 0;
                if (totalPriceList != null) {
                    Map<?, ?> fd = (Map<?, ?>) totalPriceList.get("fd");
                    if (fd != null) {
                        Map<?, ?> adult = (Map<?, ?>) fd.get("ADULT");
                        if (adult != null && adult.get("fC") instanceof Map<?, ?> fc) {
                            totalFare = fc.get("TF") instanceof Number n ? n.doubleValue() : 0;
                        }
                    }
                }

                options.add(FlightSearchResponse.FlightOption.builder()
                        .optionId((String) result.get("resultIndex"))
                        .segments(segments)
                        .fare(FlightSearchResponse.FareInfo.builder()
                                .totalFare(totalFare)
                                .totalFareAllPax(totalFare)
                                .adults(1)
                                .build())
                        .stops(segments.size() - 1)
                        .build());
            }
            return FlightSearchResponse.builder()
                    .searchId(searchId)
                    .flights(options)
                    .totalResults(options.size())
                    .currency("INR")
                    .build();
        } catch (Exception e) {
            log.warn("Failed to parse Tripjack search response: {}", e.getMessage());
            return FlightSearchResponse.builder().flights(options).currency("INR").build();
        }
    }

    private FlightFareQuoteResponse mapFareQuoteResponse(Map<?, ?> raw, String optionId) {
        try {
            Map<?, ?> reviewResult = (Map<?, ?>) raw.get("reviewResult");
            if (reviewResult == null) return FlightFareQuoteResponse.builder().available(false).build();

            Number tf = null;
            Map<?, ?> fd = (Map<?, ?>) reviewResult.get("fd");
            if (fd != null) {
                Map<?, ?> adult = (Map<?, ?>) fd.get("ADULT");
                if (adult != null && adult.get("fC") instanceof Map<?, ?> fc) {
                    tf = (Number) fc.get("TF");
                }
            }
            double totalFare = tf != null ? tf.doubleValue() : 0;
            return FlightFareQuoteResponse.builder()
                    .optionId(optionId)
                    .currentTotalFare(totalFare)
                    .totalFareAllPax(totalFare)
                    .available(true)
                    .seatsAvailable(9)
                    .build();
        } catch (Exception e) {
            log.warn("Failed to parse Tripjack fare-quote response: {}", e.getMessage());
            return FlightFareQuoteResponse.builder().available(false).build();
        }
    }

    @SuppressWarnings("unchecked")
    private FlightBookResponse mapBookResponse(Map<?, ?> raw) {
        try {
            Map<?, ?> bookingResult = (Map<?, ?>) raw.get("flightBookingResponse");
            if (bookingResult == null) return FlightBookResponse.builder().status("FAILED").build();
            return FlightBookResponse.builder()
                    .bookingId((String) bookingResult.get("orderId"))
                    .airlinePnr((String) bookingResult.get("pnr"))
                    .tripjackOrderId((String) bookingResult.get("bookingId"))
                    .status("HOLD")
                    .totalAmount(bookingResult.get("totalAmount") instanceof Number n ? n.doubleValue() : 0)
                    .currency("INR")
                    .build();
        } catch (Exception e) {
            log.warn("Failed to parse Tripjack book response: {}", e.getMessage());
            return FlightBookResponse.builder().status("FAILED").build();
        }
    }

    private FlightTicketResponse mapTicketResponse(Map<?, ?> raw) {
        try {
            Map<?, ?> ticketResult = (Map<?, ?>) raw.get("flightTicketResponse");
            if (ticketResult == null) return FlightTicketResponse.builder().status("FAILED").build();
            return FlightTicketResponse.builder()
                    .bookingId((String) ticketResult.get("orderId"))
                    .airlinePnr((String) ticketResult.get("pnr"))
                    .status("TICKETED")
                    .totalPaid(ticketResult.get("totalAmount") instanceof Number n ? n.doubleValue() : 0)
                    .build();
        } catch (Exception e) {
            log.warn("Failed to parse Tripjack ticket response: {}", e.getMessage());
            return FlightTicketResponse.builder().status("FAILED").build();
        }
    }
}

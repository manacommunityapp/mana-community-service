package com.manacommunity.api.sports.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SportsFormatDeserializer & SportsEventRequest format field tests")
class SportsFormatDeserializerTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("deserializes format from single-item JSON array (e.g. [\"TEAM\"])")
    void deserializesFormat_fromSingleItemArray() throws Exception {
        String json = """
            {
                "name": "Cricket Tournament",
                "sportId": 1,
                "communityId": 100,
                "eventDateStart": "2026-10-01",
                "eventDateEnd": "2026-10-05",
                "format": ["TEAM"]
            }
            """;

        SportsEventRequest req = objectMapper.readValue(json, SportsEventRequest.class);
        assertThat(req.getFormat()).isEqualTo("TEAM");
    }

    @Test
    @DisplayName("deserializes format from multi-item JSON array (e.g. [\"SINGLES\", \"DOUBLES\"])")
    void deserializesFormat_fromMultiItemArray() throws Exception {
        String json = """
            {
                "name": "Badminton Championship",
                "sportId": 2,
                "communityId": 100,
                "eventDateStart": "2026-10-01",
                "eventDateEnd": "2026-10-05",
                "format": ["SINGLES", "DOUBLES"]
            }
            """;

        SportsEventRequest req = objectMapper.readValue(json, SportsEventRequest.class);
        assertThat(req.getFormat()).isEqualTo("SINGLES,DOUBLES");
    }

    @Test
    @DisplayName("deserializes format from legacy plain JSON string (e.g. \"SINGLES\")")
    void deserializesFormat_fromPlainString() throws Exception {
        String json = """
            {
                "name": "Table Tennis Cup",
                "sportId": 3,
                "communityId": 100,
                "eventDateStart": "2026-10-01",
                "eventDateEnd": "2026-10-05",
                "format": "SINGLES"
            }
            """;

        SportsEventRequest req = objectMapper.readValue(json, SportsEventRequest.class);
        assertThat(req.getFormat()).isEqualTo("SINGLES");
    }

    @Test
    @DisplayName("deserializes format from empty array as null")
    void deserializesFormat_fromEmptyArray() throws Exception {
        String json = """
            {
                "name": "Marathon",
                "sportId": 4,
                "communityId": 100,
                "eventDateStart": "2026-10-01",
                "eventDateEnd": "2026-10-05",
                "format": []
            }
            """;

        SportsEventRequest req = objectMapper.readValue(json, SportsEventRequest.class);
        assertThat(req.getFormat()).isNull();
    }

    @Test
    @DisplayName("deserializes format when null or omitted")
    void deserializesFormat_whenNullOrOmitted() throws Exception {
        String jsonWithNull = """
            {
                "name": "Yoga",
                "sportId": 5,
                "communityId": 100,
                "eventDateStart": "2026-10-01",
                "eventDateEnd": "2026-10-05",
                "format": null
            }
            """;

        SportsEventRequest req1 = objectMapper.readValue(jsonWithNull, SportsEventRequest.class);
        assertThat(req1.getFormat()).isNull();

        String jsonWithoutFormat = """
            {
                "name": "Yoga",
                "sportId": 5,
                "communityId": 100,
                "eventDateStart": "2026-10-01",
                "eventDateEnd": "2026-10-05"
            }
            """;

        SportsEventRequest req2 = objectMapper.readValue(jsonWithoutFormat, SportsEventRequest.class);
        assertThat(req2.getFormat()).isNull();
    }

    @Test
    @DisplayName("programmatic setFormat with List<String> joins with comma")
    void programmaticSetFormat_withList() {
        SportsEventRequest req = new SportsEventRequest();
        req.setFormat(List.of("SINGLES", "MIXED_DOUBLES"));
        assertThat(req.getFormat()).isEqualTo("SINGLES,MIXED_DOUBLES");

        req.setFormat((List<String>) null);
        assertThat(req.getFormat()).isNull();
    }
}

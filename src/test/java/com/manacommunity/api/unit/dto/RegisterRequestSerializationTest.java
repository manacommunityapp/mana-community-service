package com.manacommunity.api.unit.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.manacommunity.api.user.dto.RegisterRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RegisterRequest Serialization & Normalization Tests")
class RegisterRequestSerializationTest {

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    @DisplayName("deserializes standard fields correctly")
    void standardPayloadDeserialization() throws Exception {
        String json = """
            {
                "fullName": "Jane Doe",
                "email": "jane@example.com",
                "phone": "9876543210",
                "inviteCode": "INVITE123",
                "password": "Password123!",
                "gender": "FEMALE",
                "flatNo": "204",
                "block": "B",
                "consentToPrivacyNotice": true
            }
            """;

        RegisterRequest req = mapper.readValue(json, RegisterRequest.class);

        assertThat(req.getFullName()).isEqualTo("Jane Doe");
        assertThat(req.getEmail()).isEqualTo("jane@example.com");
        assertThat(req.getPhone()).isEqualTo("9876543210");
        assertThat(req.getInviteCode()).isEqualTo("INVITE123");
        assertThat(req.getGender()).isEqualTo("FEMALE");
        assertThat(req.getFlatNo()).isEqualTo("204");
        assertThat(req.getBlock()).isEqualTo("B");
        assertThat(req.getConsentToPrivacyNotice()).isTrue();
    }

    @Test
    @DisplayName("deserializes legacy mobile alias fields and normalizes phone and gender")
    void legacyAliasPayloadDeserialization() throws Exception {
        String json = """
            {
                "name": "Jane Doe",
                "email": "jane@example.com",
                "mobile": "+91 98765 43210",
                "communityCode": "COMM123",
                "password": "Password123!",
                "flatNumber": "204",
                "tower": "B",
                "consentToPrivacyNotice": true
            }
            """;

        RegisterRequest req = mapper.readValue(json, RegisterRequest.class);

        // Aliases mapped
        assertThat(req.getFullName()).isEqualTo("Jane Doe");
        assertThat(req.getPhone()).isEqualTo("9876543210");
        assertThat(req.getInviteCode()).isEqualTo("COMM123");
        assertThat(req.getFlatNo()).isEqualTo("204");
        assertThat(req.getBlock()).isEqualTo("B");

        // Defaults applied
        assertThat(req.getGender()).isEqualTo("OTHER");
    }

    @Test
    @DisplayName("normalizes phone with leading zero or spaces")
    void phoneNormalization() {
        RegisterRequest req = new RegisterRequest();
        req.setPhone("09876543210");
        assertThat(req.getPhone()).isEqualTo("9876543210");

        req.setPhone("+91-98765-43210");
        assertThat(req.getPhone()).isEqualTo("9876543210");

        req.setPhone("9876543210");
        assertThat(req.getPhone()).isEqualTo("9876543210");
    }
}

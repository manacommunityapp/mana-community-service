package com.manacommunity.api.sports.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SportsCricHeroesScraperServiceTest {

    private final SportsCricHeroesScraperService scraperService = new SportsCricHeroesScraperService();

    // Mock DNS lookup that returns a public IP address (Google DNS 8.8.8.8)
    private final SportsCricHeroesScraperService.DnsLookup publicDns =
            host -> new InetAddress[]{InetAddress.getByAddress(host, new byte[]{8, 8, 8, 8})};

    @Test
    @DisplayName("Valid CricHeroes HTTPS URLs should pass validation")
    void validateSafeUrl_validCricHeroesUrl_success() {
        URI uri1 = SportsCricHeroesScraperService.validateSafeUrl("https://cricheroes.com/player-profile/123456", publicDns);
        assertThat(uri1.getHost()).isEqualTo("cricheroes.com");

        URI uri2 = SportsCricHeroesScraperService.validateSafeUrl("https://www.cricheroes.com/player-profile/987654", publicDns);
        assertThat(uri2.getHost()).isEqualTo("www.cricheroes.com");

        URI uri3 = SportsCricHeroesScraperService.validateSafeUrl("https://chshare.link/player/55555", publicDns);
        assertThat(uri3.getHost()).isEqualTo("chshare.link");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    @DisplayName("Blank or null URLs should throw IllegalArgumentException")
    void validateSafeUrl_blankUrl_throwsException(String url) {
        assertThatThrownBy(() -> SportsCricHeroesScraperService.validateSafeUrl(url, publicDns))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be empty");

        assertThatThrownBy(() -> SportsCricHeroesScraperService.validateSafeUrl(null, publicDns))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be empty");
    }

    @Test
    @DisplayName("Plain HTTP should be rejected to prevent cleartext transmission and insecure redirects")
    void validateSafeUrl_httpProtocol_rejected() {
        assertThatThrownBy(() -> SportsCricHeroesScraperService.validateSafeUrl("http://cricheroes.com/player/123", publicDns))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Only HTTPS URLs are permitted");
    }

    @Test
    @DisplayName("Non-whitelisted external domains must be blocked (SSRF attempt)")
    void validateSafeUrl_disallowedDomains_rejected() {
        assertThatThrownBy(() -> SportsCricHeroesScraperService.validateSafeUrl("https://evil.com/player/123", publicDns))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not permitted");

        assertThatThrownBy(() -> SportsCricHeroesScraperService.validateSafeUrl("https://cricheroes.com.attacker.com/player/123", publicDns))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not permitted");

        assertThatThrownBy(() -> SportsCricHeroesScraperService.validateSafeUrl("https://notcricheroes.com/player/123", publicDns))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not permitted");
    }

    @Test
    @DisplayName("Direct IP addresses and cloud metadata hosts must be rejected by domain whitelist")
    void validateSafeUrl_directIpOrMetadata_rejected() {
        assertThatThrownBy(() -> SportsCricHeroesScraperService.validateSafeUrl("https://169.254.169.254/latest/meta-data/", publicDns))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not permitted");

        assertThatThrownBy(() -> SportsCricHeroesScraperService.validateSafeUrl("https://127.0.0.1/actuator/env", publicDns))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not permitted");
    }

    @Test
    @DisplayName("URL with user credentials must be rejected")
    void validateSafeUrl_userInfo_rejected() {
        assertThatThrownBy(() -> SportsCricHeroesScraperService.validateSafeUrl("https://admin:secret@cricheroes.com/player/123", publicDns))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not contain user credentials");
    }

    @Test
    @DisplayName("Non-standard HTTPS ports must be rejected")
    void validateSafeUrl_nonStandardPort_rejected() {
        assertThatThrownBy(() -> SportsCricHeroesScraperService.validateSafeUrl("https://cricheroes.com:8443/player/123", publicDns))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Only standard HTTPS port");
    }

    @Test
    @DisplayName("DNS resolving to loopback 127.0.0.1 must be blocked (anti-SSRF)")
    void validateSafeUrl_resolvesToLoopback_blocked() {
        SportsCricHeroesScraperService.DnsLookup loopbackDns =
                host -> new InetAddress[]{InetAddress.getByAddress(host, new byte[]{127, 0, 0, 1})};

        assertThatThrownBy(() -> SportsCricHeroesScraperService.validateSafeUrl("https://cricheroes.com/player/123", loopbackDns))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("restricted internal IP address");
    }

    @Test
    @DisplayName("DNS resolving to AWS/GCP metadata 169.254.169.254 must be blocked (anti-SSRF)")
    void validateSafeUrl_resolvesToMetadata_blocked() {
        SportsCricHeroesScraperService.DnsLookup metadataDns =
                host -> new InetAddress[]{InetAddress.getByAddress(host, new byte[]{(byte) 169, (byte) 254, (byte) 169, (byte) 254})};

        assertThatThrownBy(() -> SportsCricHeroesScraperService.validateSafeUrl("https://cricheroes.com/player/123", metadataDns))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("restricted internal IP address");
    }

    @Test
    @DisplayName("DNS resolving to RFC 1918 private IPs must be blocked (anti-SSRF)")
    void validateSafeUrl_resolvesToPrivateIp_blocked() {
        SportsCricHeroesScraperService.DnsLookup privateDns10 =
                host -> new InetAddress[]{InetAddress.getByAddress(host, new byte[]{10, 0, 1, 5})};
        SportsCricHeroesScraperService.DnsLookup privateDns192 =
                host -> new InetAddress[]{InetAddress.getByAddress(host, new byte[]{(byte) 192, (byte) 168, 1, 100})};

        assertThatThrownBy(() -> SportsCricHeroesScraperService.validateSafeUrl("https://cricheroes.com/player/123", privateDns10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("restricted internal IP address");

        assertThatThrownBy(() -> SportsCricHeroesScraperService.validateSafeUrl("https://cricheroes.com/player/123", privateDns192))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("restricted internal IP address");
    }

    @Test
    @DisplayName("isRestrictedIp correctly identifies restricted addresses")
    void isRestrictedIp_identifiesCorrectly() throws UnknownHostException {
        // Restricted
        assertThat(SportsCricHeroesScraperService.isRestrictedIp(InetAddress.getByName("127.0.0.1"))).isTrue();
        assertThat(SportsCricHeroesScraperService.isRestrictedIp(InetAddress.getByName("169.254.169.254"))).isTrue();
        assertThat(SportsCricHeroesScraperService.isRestrictedIp(InetAddress.getByName("10.10.10.10"))).isTrue();
        assertThat(SportsCricHeroesScraperService.isRestrictedIp(InetAddress.getByName("192.168.0.1"))).isTrue();
        assertThat(SportsCricHeroesScraperService.isRestrictedIp(InetAddress.getByName("172.16.0.1"))).isTrue();
        assertThat(SportsCricHeroesScraperService.isRestrictedIp(InetAddress.getByName("0.0.0.0"))).isTrue();

        // Allowed public IP
        assertThat(SportsCricHeroesScraperService.isRestrictedIp(InetAddress.getByName("8.8.8.8"))).isFalse();
    }

    @Test
    @DisplayName("extractPlayerId correctly parses IDs from profile and share links")
    void extractPlayerId_formats() {
        assertThat(scraperService.extractPlayerId("https://cricheroes.com/player-profile/123456")).isEqualTo("123456");
        assertThat(scraperService.extractPlayerId("https://cricheroes.com/player/789012")).isEqualTo("789012");
        assertThat(scraperService.extractPlayerId("https://chshare.link/player/345678")).isEqualTo("345678");

        // Null url
        assertThat(scraperService.extractPlayerId(null)).isNull();

        // Fallback hash pseudo-ID
        String pseudo = scraperService.extractPlayerId("https://cricheroes.com/custom-slug");
        assertThat(pseudo).startsWith("CH-");
    }
}

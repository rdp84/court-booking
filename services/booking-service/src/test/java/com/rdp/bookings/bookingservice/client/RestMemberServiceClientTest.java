package com.rdp.bookings.bookingservice.client;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.client.RestClient;

import com.github.tomakehurst.wiremock.WireMockServer;

class RestMemberServiceClientTest {
    static WireMockServer wireMockServer;

    @BeforeAll
    static void startWireMock() {
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();
    }

    @AfterAll
    static void stopWireMock() {
        wireMockServer.stop();
    }

    private RestMemberServiceClient client;

    @BeforeEach
    void setup() {
        wireMockServer.resetAll();
        client = new RestMemberServiceClient(RestClient.builder(),
                new MemberServiceProperties("http://localhost:" + wireMockServer.port()));
    }

    @Test
    void shouldReturnMemberWhenFound() {
        final var memberId = UUID.randomUUID();
        final var testUrl = "/members/" + memberId;

        wireMockServer.stubFor(get(urlEqualTo(testUrl))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody("""
                        {
                            "id": "%s",
                            "accountBalance": 20.00,
                            "membershipStartDate": "2000-01-01",
                            "membershipEndDate": "2001-01-01"
                        }
                        """.formatted(memberId))));

        final var result = client.getMember(memberId);

        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo(memberId);
        assertThat(result.get().accountBalance()).isEqualByComparingTo(new BigDecimal("20.00"));
        wireMockServer.verify(getRequestedFor(urlEqualTo(testUrl)));
    }

    @Test
    void shouldReturnEmptyWhenMemberNotFound() {
        final var memberId = UUID.randomUUID();
        final var testUrl = "/members/" + memberId;

        wireMockServer.stubFor(get(urlEqualTo(testUrl)).willReturn(aResponse().withStatus(404)));

        final var result = client.getMember(memberId);

        assertThat(result).isNotPresent();
        wireMockServer.verify(getRequestedFor(urlEqualTo(testUrl)));
    }

    @ParameterizedTest
    @ValueSource(ints = { 400, 401, 403, 409, 422 })
    void shouldThrowUpstreamServiceExceptionForOtherClientErrors(final int status) {
        final var memberId = UUID.randomUUID();
        final var testUrl = "/members/" + memberId;

        wireMockServer.stubFor(get(urlEqualTo(testUrl)).willReturn(aResponse().withStatus(status)));

        assertThatThrownBy(() -> client.getMember(memberId)).isInstanceOf(UpstreamServiceException.class);
        wireMockServer.verify(getRequestedFor(urlEqualTo(testUrl)));
    }
}

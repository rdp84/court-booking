package com.rdp.bookings.bookingservice.client;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.client.RestClient;

import com.github.tomakehurst.wiremock.WireMockServer;

class RestCourtServiceClientTest {
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

    private RestCourtServiceClient client;

    @BeforeEach
    void setup() {
        wireMockServer.resetAll();
        client = new RestCourtServiceClient(RestClient.builder(),
                new CourtServiceProperties("http://localhost:" + wireMockServer.port()));
    }

    @Nested
    class GetCourt {

        @Test
        void shouldReturnCourtWhenFound() {
            final var courtId = UUID.randomUUID();
            final var testUrl = "/courts/" + courtId;

            wireMockServer.stubFor(get(urlEqualTo(testUrl))
                    .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody("""
                            {
                                "id": "%s",
                                "name": "Court 1",
                                "isActive": true
                            }
                            """.formatted(courtId))));

            final var result = client.getCourt(courtId);

            assertThat(result).isPresent();
            assertThat(result.get().id()).isEqualTo(courtId);
            assertThat(result.get().name()).isEqualTo("Court 1");
            assertThat(result.get().isActive()).isTrue();
            wireMockServer.verify(getRequestedFor(urlEqualTo(testUrl)));
        }

        @Test
        void shouldReturnEmptyWhenCourtNotFound() {
            final var courtId = UUID.randomUUID();
            final var testUrl = "/courts/" + courtId;

            wireMockServer.stubFor(get(urlEqualTo(testUrl)).willReturn(aResponse().withStatus(404)));

            final var result = client.getCourt(courtId);

            assertThat(result).isNotPresent();
            wireMockServer.verify(getRequestedFor(urlEqualTo(testUrl)));
        }

        @ParameterizedTest
        @ValueSource(ints = { 400, 401, 403, 409, 422 })
        void shouldThrowUpstreamServiceExceptionForOtherClientErrors(final int status) {
            final var courtId = UUID.randomUUID();
            final var testUrl = "/courts/" + courtId;

            wireMockServer.stubFor(get(urlEqualTo(testUrl)).willReturn(aResponse().withStatus(status)));

            assertThatThrownBy(() -> client.getCourt(courtId)).isInstanceOf(UpstreamServiceException.class);
            wireMockServer.verify(getRequestedFor(urlEqualTo(testUrl)));
        }
    }

    @Nested
    class GetTimeSlot {

        @Test
        void shouldReturnTimeSlotWhenFound() {
            final var timeSlotId = UUID.randomUUID();
            final var courtId = UUID.randomUUID();
            final var testUrl = "/timeslots/" + timeSlotId;

            wireMockServer.stubFor(get(urlEqualTo(testUrl))
                    .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody("""
                            {
                                "id": "%s",
                                "courtId": "%s",
                                "slotStart": "17:15:00",
                                "slotEnd": "18:00:00"
                            }
                            """.formatted(timeSlotId, courtId))));

            final var result = client.getTimeSlot(timeSlotId);

            assertThat(result).isPresent();
            assertThat(result.get().id()).isEqualTo(timeSlotId);
            assertThat(result.get().courtId()).isEqualTo(courtId);
            assertThat(result.get().slotStart()).isEqualTo(LocalTime.of(17, 15));
            assertThat(result.get().slotEnd()).isEqualTo(LocalTime.of(18, 0));
            wireMockServer.verify(getRequestedFor(urlEqualTo(testUrl)));
        }

        @Test
        void shouldReturnEmptyWhenTimeSlotNotFound() {
            final var timeSlotId = UUID.randomUUID();
            final var testUrl = "/timeslots/" + timeSlotId;

            wireMockServer.stubFor(get(urlEqualTo(testUrl)).willReturn(aResponse().withStatus(404)));

            final var result = client.getTimeSlot(timeSlotId);

            assertThat(result).isNotPresent();
            wireMockServer.verify(getRequestedFor(urlEqualTo(testUrl)));
        }

        @ParameterizedTest
        @ValueSource(ints = { 400, 401, 403, 409, 422 })
        void shouldThrowUpstreamServiceExceptionForOtherClientErrors(final int status) {
            final var timeSlotId = UUID.randomUUID();
            final var testUrl = "/timeslots/" + timeSlotId;

            wireMockServer.stubFor(get(urlEqualTo(testUrl)).willReturn(aResponse().withStatus(status)));

            assertThatThrownBy(() -> client.getTimeSlot(timeSlotId)).isInstanceOf(UpstreamServiceException.class);
            wireMockServer.verify(getRequestedFor(urlEqualTo(testUrl)));
        }
    }

    @Nested
    class GetPricing {
        private static final String PRICING_PATH = "/courts/pricing";
        private static final LocalDate DATE = LocalDate.of(2000, 1, 3);
        private static final LocalTime TIME = LocalTime.of(17, 15);

        @Test
        void shouldReturnPricingWhenFound() {
            wireMockServer.stubFor(get(urlPathEqualTo(PRICING_PATH)).withQueryParam("date", equalTo("2000-01-03"))
                    .withQueryParam("time", equalTo("17:15"))
                    .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody("""
                            {
                                "fee": 7.50
                            }
                            """)));

            final var result = client.getPricing(DATE, TIME);

            assertThat(result).isPresent();
            assertThat(result.get().fee()).isEqualByComparingTo(new BigDecimal("7.50"));
            wireMockServer.verify(getRequestedFor(urlPathEqualTo(PRICING_PATH))
                    .withQueryParam("date", equalTo("2000-01-03")).withQueryParam("time", equalTo("17:15")));
        }

        @Test
        void shouldReturnEmptyWhenPricingNotFound() {
            wireMockServer.stubFor(get(urlPathEqualTo(PRICING_PATH)).willReturn(aResponse().withStatus(404)));

            final var result = client.getPricing(DATE, TIME);

            assertThat(result).isNotPresent();
            wireMockServer.verify(getRequestedFor(urlPathEqualTo(PRICING_PATH)));
        }

        @ParameterizedTest
        @ValueSource(ints = { 400, 401, 403, 409, 422 })
        void shouldThrowUpstreamServiceExceptionForOtherClientErrors(final int status) {
            wireMockServer.stubFor(get(urlPathEqualTo(PRICING_PATH)).willReturn(aResponse().withStatus(status)));

            assertThatThrownBy(() -> client.getPricing(DATE, TIME)).isInstanceOf(UpstreamServiceException.class);
            wireMockServer.verify(getRequestedFor(urlPathEqualTo(PRICING_PATH)));
        }
    }
}

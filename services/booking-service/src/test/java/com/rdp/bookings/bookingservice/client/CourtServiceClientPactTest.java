package com.rdp.bookings.bookingservice.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTest;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;

@PactConsumerTest
@PactTestFor(providerName = "court-service")
class CourtServiceClientPactTest {

    private static final UUID EXISTENT_COURT_ID = UUID.fromString("a3f8c2d1-0000-0000-0000-000000000001");
    private static final UUID NON_EXISTENT_COURT_ID = UUID.fromString("a3f8c2d1-0000-0000-0000-000000000002");
    private static final UUID EXISTENT_TIME_SLOT_ID = UUID.fromString("a793bbf9-0000-0000-0000-000000000001");
    private static final UUID NON_EXISTENT_TIME_SLOT_ID = UUID.fromString("a793bbf9-0000-0000-0000-000000000002");

    @Pact(consumer = "booking-service")
    V4Pact getExistingCourt(final PactDslWithProvider builder) {
        return builder.given("a court exists with id " + EXISTENT_COURT_ID)
                .uponReceiving("a request for an existing court").path("/courts/" +
                        EXISTENT_COURT_ID)
                .method("GET")
                .willRespondWith().status(200).headers(Map.of("Content-Type",
                        "application/json"))
                .body(new PactDslJsonBody().uuid("id",
                        EXISTENT_COURT_ID).booleanType("isActive", true))
                .toPact(V4Pact.class);
    }

    @Pact(consumer = "booking-service")
    V4Pact getCourtNotFound(final PactDslWithProvider builder) {
        return builder.given("no court exists with id " + NON_EXISTENT_COURT_ID)
                .uponReceiving("a request for a non-existent court")
                .path("/courts/" + NON_EXISTENT_COURT_ID).method("GET")
                .willRespondWith().status(404).toPact(V4Pact.class);
    }

    @Pact(consumer = "booking-service")
    V4Pact getExistingTimeSlot(final PactDslWithProvider builder) throws ParseException {
        final var slotStart = new SimpleDateFormat("HH:mm:ss").parse("06:45:00");
        final var slotEnd = new SimpleDateFormat("HH:mm:ss").parse("07:30:00");

        return builder.given("a time slot exists with id " + EXISTENT_TIME_SLOT_ID)
                .uponReceiving("a request for an existing time slot").path("/timeslots/" +
                        EXISTENT_TIME_SLOT_ID)
                .method("GET")
                .willRespondWith().status(200).headers(Map.of("Content-Type",
                        "application/json"))
                .body(new PactDslJsonBody().uuid("id",
                        EXISTENT_TIME_SLOT_ID).uuid("courtId", EXISTENT_COURT_ID)
                        .time("slotStart", "HH:mm:ss", slotStart)
                        .time("slotEnd", "HH:mm:ss", slotEnd))
                .toPact(V4Pact.class);
    }

    @Pact(consumer = "booking-service")
    V4Pact getTimeSlotNotFound(final PactDslWithProvider builder) {
        return builder.given("no time slot exists with id " + NON_EXISTENT_TIME_SLOT_ID)
                .uponReceiving("a request for a non-existent time slot")
                .path("/timeslots/" + NON_EXISTENT_TIME_SLOT_ID).method("GET")
                .willRespondWith().status(404).toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "getExistingCourt")
    void shouldReturnCourtWhenFound(final MockServer mockServer) {
        final var client = new RestCourtServiceClient(RestClient.builder(),
                new CourtServiceProperties(mockServer.getUrl()));

        final var result = client.getCourt(EXISTENT_COURT_ID);

        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo(EXISTENT_COURT_ID);
        assertThat(result.get().isActive()).isTrue();
    }

    @Test
    @PactTestFor(pactMethod = "getCourtNotFound")
    void shouldReturnEmptyWhenCourtNotFound(final MockServer mockServer) {
        final var client = new RestCourtServiceClient(RestClient.builder(),
                new CourtServiceProperties(mockServer.getUrl()));

        final var result = client.getCourt(NON_EXISTENT_COURT_ID);

        assertThat(result).isNotPresent();
    }

    @Test
    @PactTestFor(pactMethod = "getExistingTimeSlot")
    void shouldReturnTimeSlotWhenFound(final MockServer mockServer) {
        final var client = new RestCourtServiceClient(RestClient.builder(),
                new CourtServiceProperties(mockServer.getUrl()));

        final var result = client.getTimeSlot(EXISTENT_TIME_SLOT_ID);

        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo(EXISTENT_TIME_SLOT_ID);
        assertThat(result.get().courtId()).isEqualTo(EXISTENT_COURT_ID);
        assertThat(result.get().slotStart()).isEqualTo(LocalTime.of(6, 45));
        assertThat(result.get().slotEnd()).isEqualTo(LocalTime.of(7, 30));
    }

    @Test
    @PactTestFor(pactMethod = "getTimeSlotNotFound")
    void shouldReturnEmptyWhenTimeSlotNotFound(final MockServer mockServer) {
        final var client = new RestCourtServiceClient(RestClient.builder(),
                new CourtServiceProperties(mockServer.getUrl()));

        final var result = client.getTimeSlot(NON_EXISTENT_TIME_SLOT_ID);

        assertThat(result).isNotPresent();
    }
}

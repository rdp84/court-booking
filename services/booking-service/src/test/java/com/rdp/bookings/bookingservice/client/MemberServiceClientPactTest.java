package com.rdp.bookings.bookingservice.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
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
@PactTestFor(providerName = "member-service")
class MemberServiceClientPactTest {

    private static final UUID EXISTENT_MEMBER_ID = UUID.fromString("f9c7a4d2-0000-0000-0000-000000000001");
    private static final UUID NON_EXISTENT_MEMBER_ID = UUID.fromString("f9c7a4d2-0000-0000-0000-000000000002");

    @Pact(consumer = "booking-service")
    V4Pact getExistingMember(final PactDslWithProvider builder) throws ParseException {
        final var membershipStartDate = new SimpleDateFormat("yyyy-MM-dd").parse("2000-01-01");
        final var membershipEndDate = new SimpleDateFormat("yyyy-MM-dd").parse("2001-01-01");

        return builder.given("a member exists with id " + EXISTENT_MEMBER_ID)
                .uponReceiving("a request for an existing member").path("/members/" + EXISTENT_MEMBER_ID).method("GET")
                .willRespondWith().status(200).headers(Map.of("Content-Type", "application/json"))
                .body(new PactDslJsonBody().uuid("id", EXISTENT_MEMBER_ID)
                        .decimalType("accountBalance", new BigDecimal("20.00"))
                        .date("membershipStartDate", "yyyy-MM-dd", membershipStartDate)
                        .date("membershipEndDate", "yyyy-MM-dd", membershipEndDate))
                .toPact(V4Pact.class);
    }

    @Pact(consumer = "booking-service")
    V4Pact getMemberNotFound(final PactDslWithProvider builder) throws ParseException {
        return builder.given("no member exists with id " + NON_EXISTENT_MEMBER_ID)
                .uponReceiving("a request for a non-existent member").path("/members/" + NON_EXISTENT_MEMBER_ID)
                .method("GET")
                .willRespondWith().status(404)
                .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "getExistingMember")
    void shouldReturnMemberWhenFound(final MockServer mockServer) {
        final var client = new RestMemberServiceClient(RestClient.builder(),
                new MemberServiceProperties(mockServer.getUrl()));

        final var result = client.getMember(EXISTENT_MEMBER_ID);

        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo(EXISTENT_MEMBER_ID);
        assertThat(result.get().accountBalance()).isEqualByComparingTo(new BigDecimal("20.00"));
        assertThat(result.get().membershipStartDate()).isEqualTo(LocalDate.of(2000, 1, 1));
        assertThat(result.get().membershipEndDate()).isEqualTo(LocalDate.of(2001, 1, 1));
    }

    @Test
    @PactTestFor(pactMethod = "getMemberNotFound")
    void shouldReturnEmptyWhenMemberNotFound(final MockServer mockServer) {
        final var client = new RestMemberServiceClient(RestClient.builder(),
                new MemberServiceProperties(mockServer.getUrl()));

        final var result = client.getMember(NON_EXISTENT_MEMBER_ID);

        assertThat(result).isNotPresent();
    }
}

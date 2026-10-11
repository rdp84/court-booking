package com.rdp.bookings.bookingservice.client;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Component
class RestMemberServiceClient implements MemberServiceClient {

    private final RestClient restClient;

    RestMemberServiceClient(final RestClient.Builder restClientBuilder, final MemberServiceProperties properties) {
        this.restClient = restClientBuilder.baseUrl(properties.url()).build();
    }

    @Override
    public Optional<MemberResponse> getMember(final UUID memberId) {
        try {
            final var member = restClient.get().uri("/members/{id}", memberId).retrieve().body(MemberResponse.class);
            return Optional.ofNullable(member);
        } catch (final HttpClientErrorException.NotFound e) {
            return Optional.empty();
        } catch (final HttpClientErrorException e) {
            throw new UpstreamServiceException("Unexpected response from Member Service: " + e.getStatusCode(), e);
        }
    }

    // A 404 is unexpected here too: the member was looked up moments before being charged
    @Override
    public ChargeResult chargeBooking(final UUID memberId, final UUID bookingId, final BigDecimal amount) {
        try {
            restClient.put().uri("/members/{id}/booking-payments/{bookingId}", memberId, bookingId)
                    .body(Map.of("amount", amount)).retrieve().toBodilessEntity();
            return ChargeResult.CHARGED;
        } catch (final HttpClientErrorException.UnprocessableContent e) {
            return ChargeResult.INSUFFICIENT_BALANCE;
        } catch (final HttpClientErrorException e) {
            throw new UpstreamServiceException("Unexpected response from Member Service: " + e.getStatusCode(), e);
        }
    }

    @Override
    public void refundBooking(final UUID memberId, final UUID bookingId) {
        try {
            restClient.put().uri("/members/{id}/booking-refunds/{bookingId}", memberId, bookingId).retrieve()
                    .toBodilessEntity();
        } catch (final HttpClientErrorException e) {
            throw new UpstreamServiceException("Unexpected response from Member Service: " + e.getStatusCode(), e);
        }
    }
}

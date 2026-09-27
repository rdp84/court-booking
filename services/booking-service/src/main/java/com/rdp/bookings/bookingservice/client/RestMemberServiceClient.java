package com.rdp.bookings.bookingservice.client;

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
}

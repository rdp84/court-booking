package com.rdp.bookings.bookingservice.client;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Component
class RestCourtServiceClient implements CourtServiceClient {

    private final RestClient restClient;

    RestCourtServiceClient(final RestClient.Builder restClientBuilder, final CourtServiceProperties properties) {
        this.restClient = restClientBuilder.baseUrl(properties.url()).build();
    }

    @Override
    public Optional<CourtResponse> getCourt(final UUID courtId) {
        try {
            final var court = restClient.get().uri("/courts/{id}", courtId).retrieve().body(CourtResponse.class);
            return Optional.ofNullable(court);
        } catch (final HttpClientErrorException.NotFound e) {
            return Optional.empty();
        } catch (final HttpClientErrorException e) {
            throw new UpstreamServiceException("Unexpected response from Court Service: " + e.getStatusCode(), e);
        }
    }

    @Override
    public Optional<TimeSlotResponse> getTimeSlot(final UUID timeSlotId) {
        try {
            final var timeSlot = restClient.get().uri("/timeslots/{id}", timeSlotId).retrieve()
                    .body(TimeSlotResponse.class);
            return Optional.ofNullable(timeSlot);
        } catch (final HttpClientErrorException.NotFound e) {
            return Optional.empty();
        } catch (final HttpClientErrorException e) {
            throw new UpstreamServiceException("Unexpected response from Court Service: " + e.getStatusCode(), e);
        }
    }

    @Override
    public Optional<CourtPricingResponse> getPricing(final LocalDate date, final LocalTime time) {
        try {
            final var pricing = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/courts/pricing").queryParam("date", date)
                            .queryParam("time", time).build())
                    .retrieve().body(CourtPricingResponse.class);
            return Optional.ofNullable(pricing);
        } catch (final HttpClientErrorException.NotFound e) {
            return Optional.empty();
        } catch (final HttpClientErrorException e) {
            throw new UpstreamServiceException("Unexpected response from Court Service: " + e.getStatusCode(), e);
        }
    }
}

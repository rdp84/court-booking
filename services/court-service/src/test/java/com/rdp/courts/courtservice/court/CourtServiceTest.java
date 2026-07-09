package com.rdp.courts.courtservice.court;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CourtServiceTest {

    @Mock
    CourtRepository courtRepository;

    @InjectMocks
    CourtService courtService;

    @Test
    void shouldReturnOnlyActiveCourts() {
        final var activeCourt = new Court("Court 1", true);

        given(courtRepository.findByIsActiveTrue()).willReturn(List.of(activeCourt));

        final var result = courtService.getActiveCourts();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Court 1");
        assertThat(result.get(0).isActive()).isTrue();
    }

    @Test
    void shouldReturnCourtResponseWhenCourtExists() {
        final var uuid = UUID.randomUUID();
        final var court = new Court(uuid, "Court 2", true);

        given(courtRepository.findById(uuid)).willReturn(Optional.of(court));

        final var result = courtService.getCourtById(uuid);
        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo(uuid);
        assertThat(result.get().name()).isEqualTo("Court 2");
        assertThat(result.get().isActive()).isTrue();
    }

    @Test
    void shouldReturnEmptyOptionalWhenCourtNotFound() {
        final var uuid = UUID.randomUUID();

        given(courtRepository.findById(uuid)).willReturn(Optional.empty());

        final var result = courtService.getCourtById(uuid);
        assertThat(result).isNotPresent();
    }
}

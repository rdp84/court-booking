package com.rdp.courts.courtservice.court;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.List;

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
        final var inActiveCourt = new Court("Court 2", false);

        given(courtRepository.findByIsActiveTrue()).willReturn(List.of(activeCourt));

        final var result = courtService.getActiveCourts();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Court 1");
        assertThat(result.get(0).isActive()).isTrue();
    }
}

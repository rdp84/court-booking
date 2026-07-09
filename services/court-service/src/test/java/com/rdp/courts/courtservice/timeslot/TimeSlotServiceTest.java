package com.rdp.courts.courtservice.timeslot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.BDDMockito.given;

import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.rdp.courts.courtservice.court.Court;

@ExtendWith(MockitoExtension.class)
class TimeSlotServiceTest {

    @Mock
    TimeSlotRepository timeSlotRepository;

    @InjectMocks
    TimeSlotService timeSlotService;

    @Test
    void shouldReturnTimeSlotsForACourt() {
        final var court = new Court("Court 1", true);
        final var timeSlots = List.of(new TimeSlot(court, LocalTime.of(6, 45), LocalTime.of(7, 30)),
                new TimeSlot(court, LocalTime.of(12, 45), LocalTime.of(13, 30)),
                new TimeSlot(court, LocalTime.of(17, 15), LocalTime.of(18, 0)),
                new TimeSlot(court, LocalTime.of(21, 0), LocalTime.of(21, 45)));

        given(timeSlotRepository.findByCourt(court)).willReturn(timeSlots);

        final var result = timeSlotService.getTimeSlotsForCourt(court);
        assertThat(result).hasSize(4);
        assertThat(result).extracting(TimeSlotResponse::slotStart, TimeSlotResponse::slotEnd).containsExactlyInAnyOrder(
                tuple(LocalTime.of(6, 45), LocalTime.of(7, 30)), tuple(LocalTime.of(12, 45), LocalTime.of(13, 30)),
                tuple(LocalTime.of(17, 15), LocalTime.of(18, 0)), tuple(LocalTime.of(21, 0), LocalTime.of(21, 45)));
    }

    @Test
    void shouldReturnEmptyListWhenNoTimeSlotsExistForCourt() {
        final var court = new Court("Court 3", true);

        given(timeSlotRepository.findByCourt(court)).willReturn(List.of());

        final var result = timeSlotService.getTimeSlotsForCourt(court);
        assertThat(result).isEmpty();
    }
}

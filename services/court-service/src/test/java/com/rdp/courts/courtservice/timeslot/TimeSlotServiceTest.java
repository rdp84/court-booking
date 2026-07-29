package com.rdp.courts.courtservice.timeslot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.BDDMockito.given;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
        assertThat(result).extracting(TimeSlot::getSlotStart, TimeSlot::getSlotEnd).containsExactlyInAnyOrder(
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

    @Test
    void shouldReturnTimeSlotResponseWhenTimeSlotExists() {
        final var id = UUID.randomUUID();
        final var court = new Court("Court 2", true);
        final var slotStart = LocalTime.of(18, 00);
        final var slotEnd = LocalTime.of(18, 45);
        final var timeSlot = new TimeSlot(id, court, slotStart, slotEnd);

        given(timeSlotRepository.findById(id)).willReturn(Optional.of(timeSlot));

        final var result = timeSlotService.getTimeSlotById(id);
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(id);
        assertThat(result.get().getSlotStart()).isEqualTo(slotStart);
        assertThat(result.get().getSlotEnd()).isEqualTo(slotEnd);
    }

    @Test
    void shouldReturnEmptyOptionalWhenTimeSlotNotFound() {
        final var id = UUID.randomUUID();

        given(timeSlotRepository.findById(id)).willReturn(Optional.empty());

        final var result = timeSlotService.getTimeSlotById(id);
        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmptyOptionalWhenCourtIsInactive() {
        final var id = UUID.randomUUID();
        final var court = new Court("Court 2", false);
        final var slotStart = LocalTime.of(19, 30);
        final var slotEnd = LocalTime.of(20, 15);
        final var timeSlot = new TimeSlot(id, court, slotStart, slotEnd);

        given(timeSlotRepository.findById(id)).willReturn(Optional.of(timeSlot));

        final var result = timeSlotService.getTimeSlotById(id);
        assertThat(result).isEmpty();
    }
}

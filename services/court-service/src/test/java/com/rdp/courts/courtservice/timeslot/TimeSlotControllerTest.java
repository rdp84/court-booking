package com.rdp.courts.courtservice.timeslot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.rdp.courts.courtservice.court.Court;
import com.rdp.courts.courtservice.court.CourtService;

@WebMvcTest(TimeSlotController.class)
class TimeSlotControllerTest {

    @Autowired
    MockMvcTester mockMvc;

    @MockitoBean
    TimeSlotService timeSlotService;

    @MockitoBean
    CourtService courtService;

    @Test
    void shouldReturnTimeSlotsForExistingCourt() {
        final var courtId = UUID.randomUUID();
        final var court = new Court(courtId, "Court 1", true);
        final var timeSlots = List.of(new TimeSlot(court, LocalTime.of(6, 45), LocalTime.of(7, 30)),
                new TimeSlot(court, LocalTime.of(12, 45), LocalTime.of(13, 30)),
                new TimeSlot(court, LocalTime.of(17, 15), LocalTime.of(18, 0)),
                new TimeSlot(court, LocalTime.of(21, 0), LocalTime.of(21, 45)));

        given(courtService.getCourtById(courtId)).willReturn(Optional.of(court));
        given(timeSlotService.getTimeSlotsForCourt(court)).willReturn(timeSlots);

        final var body = assertThat(mockMvc.get().uri("/courts/" + courtId + "/slots")).hasStatusOk().bodyJson();
        body.extractingPath("$[0].slotStart").isEqualTo("06:45:00");
        body.extractingPath("$[0].slotEnd").isEqualTo("07:30:00");
    }

    @Test
    void shouldReturnA200ForEmptyTimeSlotsList() throws Exception {
        final var courtId = UUID.randomUUID();
        final var court = new Court(courtId, "Court 3", true);

        given(courtService.getCourtById(courtId)).willReturn(Optional.of(court));
        given(timeSlotService.getTimeSlotsForCourt(court)).willReturn(List.of());

        final var body = assertThat(mockMvc.get().uri("/courts/" + courtId + "/slots")).hasStatusOk().bodyJson();
        body.isEqualTo("[]");
    }

    @Test
    void shouldReturnA404ForNonExistentCourt() throws Exception {
        final var courtId = UUID.randomUUID();

        given(courtService.getCourtById(courtId)).willReturn(Optional.empty());

        assertThat(mockMvc.get().uri("/courts/" + courtId + "/slots")).hasStatus(404);
        verify(timeSlotService, never()).getTimeSlotsForCourt(any());
    }
}

package com.rdp.courts.courtservice.timeslot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.rdp.courts.courtservice.court.Court;

@WebMvcTest(TimeSlotLookupController.class)
class TimeSlotLookupControllerTest {

    @Autowired
    MockMvcTester mockMvc;

    @MockitoBean
    TimeSlotService timeSlotService;

    @Test
    void shouldReturnTimeSlotWhenFound() {
        final var timeSlotId = UUID.randomUUID();
        final var courtId = UUID.randomUUID();
        final var court = new Court(courtId, "Court 2", true);
        final var slotStart = LocalTime.of(12, 45);
        final var slotEnd = LocalTime.of(13, 30);
        final var timeSlot = new TimeSlot(timeSlotId, court, slotStart, slotEnd);

        given(timeSlotService.getTimeSlotById(timeSlotId)).willReturn(Optional.of(timeSlot));

        assertThat(mockMvc.get().uri("/timeslots/{id}", timeSlotId)).hasStatusOk().bodyJson().isLenientlyEqualTo("""
                    {
                        "id": "%s",
                        "courtId": "%s",
                        "slotStart": "12:45:00",
                        "slotEnd": "13:30:00"
                    }
                """.formatted(timeSlotId, courtId));
    }

    @Test
    void shouldReturnNotFoundWhenTimeSlotDoesNotExist() {
        final var id = UUID.randomUUID();
        given(timeSlotService.getTimeSlotById(id)).willReturn(Optional.empty());

        assertThat(mockMvc.get().uri("/timeslots/{id}", id)).hasStatus(404).bodyText().isEmpty();
    }
}

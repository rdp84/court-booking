package com.rdp.bookings.bookingservice.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(BookingController.class)
class BookingControllerTest {
    private static final UUID BOOKING_ID = UUID.randomUUID();
    private static final UUID COURT_ID = UUID.randomUUID();
    private static final UUID TIME_SLOT_ID = UUID.randomUUID();
    private static final UUID BOOKER_ID = UUID.randomUUID();
    private static final UUID OPPONENT_ID = UUID.randomUUID();
    private static final LocalDate BOOKING_DATE = LocalDate.of(2030, 1, 7);

    private static Stream<Arguments> missingFieldScenarios() {
        return Stream.of(Arguments.of("courtId"), Arguments.of("timeSlotId"), Arguments.of("bookingDate"),
                Arguments.of("bookerMemberId"));
    }

    private static Stream<Arguments> rejectionScenarios() {
        return Stream.of(Arguments.of(RejectionReason.BOOKING_DATE_IN_PAST, 400),
                Arguments.of(RejectionReason.OPPONENT_IS_BOOKER, 400),
                Arguments.of(RejectionReason.COURT_NOT_FOUND, 404),
                Arguments.of(RejectionReason.TIME_SLOT_NOT_FOUND, 404),
                Arguments.of(RejectionReason.MEMBER_NOT_FOUND, 404),
                Arguments.of(RejectionReason.COURT_INACTIVE, 422),
                Arguments.of(RejectionReason.TIME_SLOT_NOT_ON_COURT, 422),
                Arguments.of(RejectionReason.NO_PRICING, 422),
                Arguments.of(RejectionReason.MEMBERSHIP_INACTIVE, 422),
                Arguments.of(RejectionReason.SLOT_ALREADY_BOOKED, 409),
                Arguments.of(RejectionReason.MEMBER_HAS_OVERLAPPING_BOOKING, 409),
                Arguments.of(RejectionReason.BOOKING_NOT_FOUND, 404),
                Arguments.of(RejectionReason.NOT_BOOKER, 403),
                Arguments.of(RejectionReason.ALREADY_CANCELLED, 409),
                Arguments.of(RejectionReason.BOOKING_ALREADY_STARTED, 422));
    }

    @Autowired
    MockMvcTester mockMvc;

    @MockitoBean
    BookingService bookingService;

    @Test
    void shouldCreateBookingAndReturn201() {
        given(bookingService.createBooking(COURT_ID, TIME_SLOT_ID, BOOKING_DATE, BOOKER_ID, OPPONENT_ID))
                .willReturn(newBooking(OPPONENT_ID));

        assertThat(mockMvc.post().uri("/bookings").contentType(MediaType.APPLICATION_JSON).content("""
                    {
                        "courtId": "%s",
                        "timeSlotId": "%s",
                        "bookingDate": "2030-01-07",
                        "bookerMemberId": "%s",
                        "opponentMemberId": "%s"
                    }
                """.formatted(COURT_ID, TIME_SLOT_ID, BOOKER_ID, OPPONENT_ID))).hasStatus(201)
                .hasHeader("Location", "/bookings/" + BOOKING_ID).bodyJson().isLenientlyEqualTo("""
                    {
                        "id": "%s",
                        "courtId": "%s",
                        "timeSlotId": "%s",
                        "slotStart": "06:45:00",
                        "slotEnd": "07:30:00",
                        "bookingDate": "2030-01-07",
                        "bookerMemberId": "%s",
                        "opponentMemberId": "%s",
                        "status": "CONFIRMED",
                        "courtFee": 6.00
                    }
                """.formatted(BOOKING_ID, COURT_ID, TIME_SLOT_ID, BOOKER_ID, OPPONENT_ID));
    }

    @Test
    void shouldCreateBookingWithGuestOpponentWhenOpponentOmitted() {
        given(bookingService.createBooking(COURT_ID, TIME_SLOT_ID, BOOKING_DATE, BOOKER_ID, null))
                .willReturn(newBooking(null));

        assertThat(mockMvc.post().uri("/bookings").contentType(MediaType.APPLICATION_JSON).content("""
                    {
                        "courtId": "%s",
                        "timeSlotId": "%s",
                        "bookingDate": "2030-01-07",
                        "bookerMemberId": "%s"
                    }
                """.formatted(COURT_ID, TIME_SLOT_ID, BOOKER_ID))).hasStatus(201).bodyJson()
                .extractingPath("$.opponentMemberId").isNull();
        verify(bookingService).createBooking(COURT_ID, TIME_SLOT_ID, BOOKING_DATE, BOOKER_ID, null);
    }

    @ParameterizedTest(name = "missing {0}")
    @MethodSource("missingFieldScenarios")
    void shouldReturnBadRequestWhenRequiredFieldMissing(final String missingField) {
        final var fields = new LinkedHashMap<String, String>();
        fields.put("courtId", "\"" + COURT_ID + "\"");
        fields.put("timeSlotId", "\"" + TIME_SLOT_ID + "\"");
        fields.put("bookingDate", "\"2030-01-07\"");
        fields.put("bookerMemberId", "\"" + BOOKER_ID + "\"");
        fields.remove(missingField);
        final var body = fields.entrySet().stream().map(e -> "\"" + e.getKey() + "\": " + e.getValue())
                .collect(Collectors.joining(", ", "{", "}"));

        assertThat(mockMvc.post().uri("/bookings").contentType(MediaType.APPLICATION_JSON).content(body))
                .hasStatus(400);
        verifyNoInteractions(bookingService);
    }

    @Test
    void shouldReturnBadRequestWhenBookingDateMalformed() {
        assertThat(mockMvc.post().uri("/bookings").contentType(MediaType.APPLICATION_JSON).content("""
                    {
                        "courtId": "%s",
                        "timeSlotId": "%s",
                        "bookingDate": "07/01/2030",
                        "bookerMemberId": "%s"
                    }
                """.formatted(COURT_ID, TIME_SLOT_ID, BOOKER_ID))).hasStatus(400);
        verifyNoInteractions(bookingService);
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("rejectionScenarios")
    void shouldMapRejectionReasonToStatus(final RejectionReason reason, final int expectedStatus) {
        given(bookingService.createBooking(any(), any(), any(), any(), any()))
                .willThrow(new BookingRejectedException(reason, "Rejected: " + reason));

        assertThat(mockMvc.post().uri("/bookings").contentType(MediaType.APPLICATION_JSON).content("""
                    {
                        "courtId": "%s",
                        "timeSlotId": "%s",
                        "bookingDate": "2030-01-07",
                        "bookerMemberId": "%s"
                    }
                """.formatted(COURT_ID, TIME_SLOT_ID, BOOKER_ID))).hasStatus(expectedStatus).bodyJson()
                .isLenientlyEqualTo("""
                    {
                        "status": %d,
                        "detail": "Rejected: %s",
                        "reason": "%s"
                    }
                """.formatted(expectedStatus, reason, reason));
    }

    @Test
    void shouldReturnBookingWhenFound() {
        given(bookingService.getBookingById(BOOKING_ID)).willReturn(Optional.of(newBooking(OPPONENT_ID)));

        assertThat(mockMvc.get().uri("/bookings/{id}", BOOKING_ID)).hasStatusOk().bodyJson().isLenientlyEqualTo("""
                    {
                        "id": "%s",
                        "courtId": "%s",
                        "bookerMemberId": "%s",
                        "opponentMemberId": "%s",
                        "status": "CONFIRMED"
                    }
                """.formatted(BOOKING_ID, COURT_ID, BOOKER_ID, OPPONENT_ID));
    }

    @Test
    void shouldReturnNotFoundWhenBookingDoesNotExist() {
        final var id = UUID.randomUUID();
        given(bookingService.getBookingById(id)).willReturn(Optional.empty());

        assertThat(mockMvc.get().uri("/bookings/{id}", id)).hasStatus(404);
    }

    @Test
    void shouldReturnBadRequestWhenBookingIdMalformed() {
        assertThat(mockMvc.get().uri("/bookings/not-a-uuid")).hasStatus(400);
        verifyNoInteractions(bookingService);
    }

    @Test
    void shouldCancelBookingAndReturnIt() {
        final var booking = newBooking(OPPONENT_ID);
        booking.setStatus(BookingStatus.CANCELLED_FULL_REFUND);
        booking.setCancelledAt(LocalDateTime.of(2030, 1, 5, 9, 30));
        given(bookingService.cancelBooking(BOOKING_ID, BOOKER_ID)).willReturn(booking);

        assertThat(mockMvc.post().uri("/bookings/{id}/cancel", BOOKING_ID).contentType(MediaType.APPLICATION_JSON)
                .content("""
                            {
                                "memberId": "%s"
                            }
                        """.formatted(BOOKER_ID))).hasStatusOk().bodyJson().isLenientlyEqualTo("""
                    {
                        "id": "%s",
                        "status": "CANCELLED_FULL_REFUND",
                        "cancelledAt": "2030-01-05T09:30:00"
                    }
                """.formatted(BOOKING_ID));
    }

    @Test
    void shouldReturnBadRequestWhenCancelMemberIdMissing() {
        assertThat(mockMvc.post().uri("/bookings/{id}/cancel", BOOKING_ID).contentType(MediaType.APPLICATION_JSON)
                .content("{}")).hasStatus(400);
        verifyNoInteractions(bookingService);
    }

    @Test
    void shouldReturnForbiddenWhenCancelledByMemberWhoIsNotTheBooker() {
        given(bookingService.cancelBooking(BOOKING_ID, OPPONENT_ID)).willThrow(
                new BookingRejectedException(RejectionReason.NOT_BOOKER, "Only the booker can cancel"));

        assertThat(mockMvc.post().uri("/bookings/{id}/cancel", BOOKING_ID).contentType(MediaType.APPLICATION_JSON)
                .content("""
                            {
                                "memberId": "%s"
                            }
                        """.formatted(OPPONENT_ID))).hasStatus(403).bodyJson().extractingPath("$.reason")
                .isEqualTo("NOT_BOOKER");
    }

    private Booking newBooking(final UUID opponentMemberId) {
        final var booking = new Booking(COURT_ID, TIME_SLOT_ID, LocalTime.of(6, 45), LocalTime.of(7, 30), BOOKER_ID,
                opponentMemberId, BOOKING_DATE, BookingStatus.CONFIRMED, new BigDecimal("6.00"));
        ReflectionTestUtils.setField(booking, "id", BOOKING_ID);
        return booking;
    }
}

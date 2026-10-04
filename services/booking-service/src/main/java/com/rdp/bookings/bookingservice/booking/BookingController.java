package com.rdp.bookings.bookingservice.booking;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/bookings")
class BookingController {
    private final BookingService bookingService;

    BookingController(final BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    ResponseEntity<BookingResponse> createBooking(@Valid @RequestBody final CreateBookingRequest request) {
        final var booking = bookingService.createBooking(request.courtId(), request.timeSlotId(),
                request.bookingDate(), request.bookerMemberId(), request.opponentMemberId());
        final var uri = URI.create("/bookings/" + booking.getId());
        return ResponseEntity.created(uri).body(toBookingResponse(booking));
    }

    @GetMapping("/{id}")
    ResponseEntity<BookingResponse> getBooking(@PathVariable final UUID id) {
        return bookingService.getBookingById(id).map(this::toBookingResponse).map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/cancel")
    BookingResponse cancelBooking(@PathVariable final UUID id, @Valid @RequestBody final CancelBookingRequest request) {
        return toBookingResponse(bookingService.cancelBooking(id, request.memberId()));
    }

    private BookingResponse toBookingResponse(final Booking booking) {
        return new BookingResponse(booking.getId(), booking.getCourtId(), booking.getTimeSlotId(),
                booking.getSlotStart(), booking.getSlotEnd(), booking.getBookingDate(), booking.getBookerMemberId(),
                booking.getOpponentMemberId(), booking.getStatus(), booking.getCourtFee(), booking.getCreatedAt(),
                booking.getCancelledAt());
    }
}
